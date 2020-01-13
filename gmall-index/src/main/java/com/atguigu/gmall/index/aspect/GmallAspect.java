package com.atguigu.gmall.index.aspect;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallAspect {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around(value = "@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        List<Object> pid = Arrays.asList(joinPoint.getArgs());

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class returnType = signature.getReturnType();
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        String s = redisTemplate.opsForValue().get(prefix + pid);
        if (!StringUtils.isEmpty(s)) {
            return JSON.parseObject(s, returnType);
        }
        String lockName = gmallCache.lockName();
        RLock fairLock = redissonClient.getFairLock(lockName + pid);
        fairLock.lock();
        String s2 = redisTemplate.opsForValue().get(prefix + pid);
        if (!StringUtils.isEmpty(s)) {
            fairLock.unlock();
            return JSON.parseObject(s2, returnType);
        }
        Object result = joinPoint.proceed(joinPoint.getArgs());
        int timeout = gmallCache.timeout();
        int bound = gmallCache.bound();
        redisTemplate.opsForValue().set(prefix + pid, JSON.toJSONString(result), timeout + new Random().nextInt(bound), TimeUnit.MINUTES);
        fairLock.unlock();
        return result;
    }

}
