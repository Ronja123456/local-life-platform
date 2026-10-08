package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式不正确");
         //符合 生成验证码
        String code= RandomUtil.randomNumbers(6);
        //保留验证码到redis
        /*
        * 验证码的key
        * 验证码的值
        * 过期时间：2
        * 时间单位：分钟
        * stringRedisTemplate : key和value 都按String 序列化
        * 对比RedisTemplate 默认JDK序列化 容易乱码
        * */
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code,
                2, TimeUnit.MINUTES);
        log.info("发送验证码成功，验证码：{}", code);


        return Result.ok("验证码发送成功");

    }
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String code=loginForm.getCode();
        String phone=loginForm.getPhone();
        //校验手机号
        if(RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式不正确");

        //校验code
        String cacheCode=stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(cacheCode==null||!cacheCode.equals(code)) {

            return Result.fail("验证码错误,请重新输入");
        }
        //一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();

        //判断用户是否存在

        //不存在，创建新用户，保存到数据库
        if(user==null){
            user=createUserWithPhone(phone);
        }

        //if存在  保存到redis ，
        //生成token 作为登录令牌
        String token= UUID.randomUUID().toString();
       // 将user对象转为Hash存储
        // 把 User 转成 UserDTO（只存必要字段，不存密码等敏感信息）
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 转换为 Map，因为要存进 Redis 的 Hash 结构
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),  //把java对象（userDTO)转为Map
                /* Hutool 的 BeanUtil 类
               BeanUtil.beanToMap(对象, 目标Map, 转换配置)*/
                           CopyOptions.create().setIgnoreNullValue(true)
                                   //转换规则配置对象    设成true 可以忽略空值null ,,null 会占内存
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
/*
          (fieldName, fieldValue) -> fieldValue.toString()
            ↑参数1      ↑参数2            ↑方法体      把每个字段的值都转成 String。
为什么必须转 String？
Redis 的 Hash 存的 value 只能是字符串
如果 id 是 Long 类型直接存，stringRedisTemplate 序列化时会报错
所以统一转成 String 再存
 * */

        //存储
        // 存进 Redis：用 Hash 结构，key = login:token:xxx
        String tokenKey=RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,map);
        //设置 token 有效期（30 分钟）
        stringRedisTemplate.expire(tokenKey,30,TimeUnit.MINUTES);
        //8.返回token
        return Result.ok(token);



    }


    @Override
    public Result sign(){
        //1.获取当前登录用户
        Long userId= UserHolder.getUser().getId();

        //2.获取日期
        LocalDateTime now=LocalDateTime.now();
        //3.拼接key
        String keySuffix=now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key=USER_SIGN_KEY+userId+keySuffix;

        //4.获取今天是这个月的第几天
        int dayOfMonth=now.getDayOfMonth();
        //5.写入redis setbit key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        log.info("签到成功");

        return Result.ok();
    }


    @Override
    public Result signCount() {

        //1，获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2. 获取日期
        LocalDateTime now = LocalDateTime.now();
        //3.拼接Key
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        //4.获取今天是这个月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //5. 获取本月截至今天所有的签到记录，返回的是一个十进制的数字

        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        if(result==null||result.isEmpty()){
            //没有任何签到结果
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num==0||num==null){
            return Result.ok(0);
        }




        //6.循环遍历
        int count=0;
        while (true) {
            //让这个数字与1做与运算，得到数字的最后一个bit位，判断这个bit是否为0
            if((num&1)==0) {
                //如果为0，未签到
                break;
            }else {
                //如果不为0，已签到，计算器+1
                count++;
                //把数字右移一位，抛弃最后一个bit位，继续下一个bit位
                num>>>=1;
            }
        }



        return Result.ok(count);
    }
















    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("user" + phone);
        //保存用户到数据库
        save(user);
        return user;
    }






}
