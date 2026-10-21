package com.taskify.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * 基础实体：统一主键（UUID v7，应用生成）与时间戳。
 * 业务实体继承本类，配合 {@link com.taskify.common.mybatis.MybatisPlusConfig} 自动填充。
 */
@Getter
@Setter
public abstract class BaseEntity {

    /** 主键，UUID v7 字符串，插入时由 MetaObjectHandler 生成。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /** 更新时间，插入与更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
