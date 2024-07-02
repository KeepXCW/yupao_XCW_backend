package com.xcw.yupao.model.dto;

import com.xcw.yupao.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 *
 * dto包里面是业务封装类
 *
 * TeamQuery 队伍查询请求 封装类（去除掉Team中不重要的信息）
 * @author xcw
 * 继承了PageRequest里面有分页数据属性
 */
@EqualsAndHashCode(callSuper=true)//当派生类（子类）使用@Data注解时，也要用这个
@Data
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;


    /**
     * id列表
     * 暂时只有查看自己创建的队伍时用到
     */
    private List<Long> idList;

    /**
     * 队伍创建人
     * 默认等于队伍第一个userId
     * xcw加
     */
    private long creatUserId;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 搜索关键词（同时对队伍名称和描述搜索）
     */
    private String searchText;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id（队伍创建人或者成员）
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
