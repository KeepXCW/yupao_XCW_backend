package com.xcw.yupao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xcw.yupao.model.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
* @author xcw
* @description 针对表【user】的数据库操作Mapper
* @createDate 2024-04-23 16:18:32
* @Entity com.yupi.usercenter.model.User
*/

@Mapper
public interface UserMapper extends BaseMapper<User> {

}




