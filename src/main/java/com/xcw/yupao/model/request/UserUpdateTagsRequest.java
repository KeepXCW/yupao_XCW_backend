package com.xcw.yupao.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户修改标签请求体
 *
 * @author Alonso
 */
@Data
public class UserUpdateTagsRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * id
     */
    private Long id;

    /**
     * 标签
     */
    private List<String> tags;
}
