package com.xcw.yupao.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍
 * @TableName team
 */
@TableName(value ="team5")
@Data
public class Team implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     * 不是设置的队伍最大人数标准，而是当前队伍人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;


    /**
     * 用户id这里是队长，但不一定是创建人，因为队长退出时，队长会根据加入时间顺延给下一个队员，创建人只是第一个队长）
     * // TODO: 2024/6/25 加一个创建人的id，创建人的id默认等于第一次队伍建立人的id也就是userTd
     *     //  //private long creatUserId;
     *     //加创建人的目的，便于不良队伍的溯源，即使删除也是逻辑删除，后台管理员仍可查，不一定非得展现给用户
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    /**
     * 是否逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 队伍头像
     */
    private String teamUrl;

    /**
     * 已加入队伍人数
     */
    private Integer hasJoinNum;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}