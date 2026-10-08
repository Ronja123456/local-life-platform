-- 参数
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1. 库存判断
if (tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- 2. 一人一单判断
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 3. 扣库存
redis.call('incrby', stockKey, -1)

-- 4. 记录用户
redis.call('sadd', orderKey, userId)

-- 5. 成功
return 0