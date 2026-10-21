package com.taskify.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.taskify.common.base.UuidV7;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;

/**
 * Mybatis-Plus 公共配置：UUID v7 主键生成（插入时）、时间戳自动填充、分页插件。
 */
@Configuration
public class MybatisPlusConfig {

    /** 主键 UUID v7 与创建/更新时间自动填充。 */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                if (metaObject.getValue("id") == null) {
                    metaObject.setValue("id", UuidV7.randomUuid());
                }
                OffsetDateTime now = OffsetDateTime.now();
                this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", OffsetDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
            }
        };
    }

    /** 分页插件（页码分页约定）。 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
