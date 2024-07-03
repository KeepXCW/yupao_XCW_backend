package com.xcw.yupao.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.yupao.model.domain.Team;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.model.dto.TeamQuery;
import com.xcw.yupao.model.request.TeamJoinRequest;
import com.xcw.yupao.model.request.TeamQuitRequest;
import com.xcw.yupao.model.request.TeamUpdateRequest;
import com.xcw.yupao.model.vo.TeamUserVO;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
* @author xcw
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-06-11 20:59:20
*/
public interface TeamService extends IService<Team> {


    /**
     *   添加队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(@RequestBody Team team, User loginUser);

    /**
     * 搜索队伍并展示队伍成员
     * @param teamQuery
     * @return
     */

    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);



    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 删除(解散)队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);

    /**
     * 分页查询队伍
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    Page<TeamUserVO> listTeamsBypage(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 查询私有队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVO> listPrivateTeams(TeamQuery teamQuery,HttpServletRequest request);

}
