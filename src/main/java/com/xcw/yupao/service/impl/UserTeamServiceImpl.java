package com.xcw.yupao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.yupao.model.domain.UserTeam;
import com.xcw.yupao.mapper.UserTeamMapper;
import com.xcw.yupao.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author xcw
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-06-11 21:05:10
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




