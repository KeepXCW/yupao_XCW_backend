package com.xcw.yupao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求参数
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 随机生成的序列化id，使此类的对象在序列化时保持唯一
     */
    private static final long serialVersionUID = -8146470886155810874L;
    /**
     * 页面大小
     * 默认是10 防止为空后续不用做非空判断，但是查询时如果不输入，空值则会覆盖，然后查询所有
     */
    protected int pageSize = 10;

    /**
     * 当前是第几页
     * 默认第一页 后续不用做非空判断
     */
    protected int pageNum = 1;
}
