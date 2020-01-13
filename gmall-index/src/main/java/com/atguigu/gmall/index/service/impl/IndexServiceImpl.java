package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService{
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PRE = "index:cates:";
    @Override
    public List<CategoryEntity> queryLe1Category() {
        Resp<List<CategoryEntity>> categories = pmsClient.getCategories(1, null);
        List<CategoryEntity> categoryEntities = categories.getData();
        return categoryEntities;
    }

    @Override
    @GmallCache(prefix = KEY_PRE,timeout = 30)
    public List<CategoryVo> queryCateWithSub(Long pid) {
        Resp<List<CategoryVo>> listResp = pmsClient.queryCateWithSub(pid);
        List<CategoryVo> categoryVos = listResp.getData();
        return categoryVos;
    }

    @Override
    public void testLock() {
        String s = UUID.randomUUID().toString();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", s, 5l+new Random().nextInt(5), TimeUnit.SECONDS);
        if (flag){
            String num = stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isEmpty(num)){
                return;
            }
            int i = Integer.parseInt(num);
            stringRedisTemplate.opsForValue().set("num",String.valueOf(++i));
//            String lock = stringRedisTemplate.opsForValue().get("lock");
//            if (StringUtils.equals(s,lock)){
//                stringRedisTemplate.delete("lock");
//            }
            String r = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(new DefaultRedisScript<>(r,Long.class), Arrays.asList("lock"),s);
        }else {
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
