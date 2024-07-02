package com.xcw.yupao.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mybatisPlus封装分页插件配置类
 */
@Configuration
@MapperScan("com.xcw.yupao.model.request.mapper")
public class MybatisPlusConfig {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor = false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
    /*
    MybatisConfiguration#useDeprecatedExecutor 是 MyBatis 配置中的一个属性，
    它用于指定是否使用旧的执行器 Executor。在 MyBatis 3 之前的版本中，默认的执行器是 SIMPLE，而在 MyBatis 3 中引入了新的执行器 REUSE 和 BATCH。
    如果设置为 true，则会使用旧的执行器，这可能会导致缓存的问题。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }
}