package com.xcw.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.yupao.common.BaseResponse;
import com.xcw.yupao.common.DeleteRequest;
import com.xcw.yupao.common.ErrorCode;
import com.xcw.yupao.common.ResultUtils;
import com.xcw.yupao.exception.BusinessException;
import com.xcw.yupao.model.domain.Team;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.model.domain.UserTeam;
import com.xcw.yupao.model.dto.TeamQuery;
import com.xcw.yupao.model.request.TeamAddRequest;
import com.xcw.yupao.model.request.TeamJoinRequest;
import com.xcw.yupao.model.request.TeamQuitRequest;
import com.xcw.yupao.model.request.TeamUpdateRequest;
import com.xcw.yupao.model.vo.TeamUserVO;
import com.xcw.yupao.model.vo.UserVO;
import com.xcw.yupao.service.TeamService;
import com.xcw.yupao.service.UserService;
import com.xcw.yupao.service.UserTeamService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 此类用于编写RESTful风格的API，并使返回值默认为JSON类型。
 * 该注解是Spring框架提供，用于标记类作为控制器，
 * 其中所有的方法都映射为HTTP请求的处理方法。
 *
 * @author xcw
 */
@RestController
@Api(tags = "队伍")//定义knife4j文档中这个类接口目录名字
@RequestMapping("/team")
/**
 * allowCredentials = "true" 是 @CrossOrigin 注解中的一个属性，
 * 它的作用是允许浏览器发送和接收跨域请求时携带凭证信息（例如Cookies、HTTP认证头或TLS客户端证书）。*/


@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    //创建队伍
    // TODO: 2024/6/25  最后优化看TeamAddRequest的userId属性，在别的地方是否有用，是否可以去掉
    //  TeamAddRequest里封装的userId属性前端不用传参，传不传默认都是队伍创建人id不优化也可以前端不传就行了

    /**
     * @param teamAddRequest
     * @param request
     * @return 备注：teamAddRequest的userId属性不用传值，始终是当前用户的id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前用户信息
        User loginUser = userService.getLoginUser(request);

        //把当前teamAddRequest的属性赋值给team对象，因为service层只有team的接口，只能对service进行crud，且参数也要和service层一致
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);

        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    //删除队伍
//    @PostMapping("/delete")
//    public BaseResponse<Boolean> deleteTeam(@RequestBody long id) {
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean result = teamService.removeById(id);
//        if (!result) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
//        }
//        return ResultUtils.success(true);
//    }



    /**
     * 修改队伍
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    //根据id查询队伍
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    //查询队伍
    /*@GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //Team team = new Team();
        //把teamQuery的属性传递给此时的team对象，因为teamService里面带的curdApi是关于Team的
//       BeanUtils.copyProperties(team,teamQuery);
//
//       QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        //获取当前用户的登录信息

        //判断当前用户是否为管理员
        boolean isAdmin = userService.isAdmin(request);
        //查询所有队伍，只有管理员才能查看非公开的队伍
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);

        //返回前端——>用户是否已加入队伍的标识team.setHasJoin(hasJoin);
        //判断当前用户是否已加入队伍
        //获取当前用户加入的队伍id集合
        *//**未用lambda表达式的写法
         * final List<Long> teamIdList = new ArrayList<>();
         *         for (TeamUserVO teamUserVO : teamList) {
         *             Long id = teamUserVO.getId();
         *             teamIdList.add(id);
         *         }
         *
         * lambda表达式第一次简化：teamList.stream().map(teamUserVO -> teamUserVO.getId()).collect(Collectors.toList());
         * 二次简化：teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
         *//*
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        //获取当前用户加入的队伍id集合
        try {
            //获取当前登录用户信息(用trycatch捕获getLoginUser判断用户未登录时异常，使此接口不登录也可使用)
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                //便于前端根据用户是否已加入来判断显示加入还是退出按钮
                team.setHasJoin(hasJoin);
            });

        }catch (Exception e){}



        return ResultUtils.success(teamList);
    }*/

    /**
     * 查询所有队伍
     * 注：team.setHasJoin(hasJoin);//便于前端根据用户是否已加入来判断显示加入还是退出按钮
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断当前用户是否为管理员
        boolean isAdmin = userService.isAdmin(request);
        //1.查询所有队伍，只有管理员才能查看非公开的队伍
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, isAdmin);

        //返回前端——>用户是否已加入队伍的标识team.setHasJoin(hasJoin);

        /**未用lambda表达式的写法
         * final List<Long> teamIdList = new ArrayList<>();
         *         for (TeamUserVO teamUserVO : teamList) {
         *             Long id = teamUserVO.getId();
         *             teamIdList.add(id);
         *         }
         *
         * lambda表达式第一次简化：teamList.stream().map(teamUserVO -> teamUserVO.getId()).collect(Collectors.toList());
         * 二次简化：teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
         */
        //2.获取当前用户加入的队伍id的集合
        final List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        //获取当前用户加入的队伍id集合
        try {
            //获取当前登录用户信息(用trycatch捕获getLoginUser判断用户未登录时异常，使此接口不登录也可使用)
            User loginUser = userService.getLoginUser(request);
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIdList);
            //得到当前用户加入的队伍
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
            // 当前用户已加入的队伍 id 集合
            Set<Long> hasJoinTeamIdSet = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            teamList.forEach(team -> {
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                //便于前端根据用户是否已加入来判断显示加入还是退出按钮
                team.setHasJoin(hasJoin);
            });

        }catch (Exception e){}
        //3. 查询加入队伍的人数
        QueryWrapper<UserTeam> userTeamJoinQueryWrapper = new QueryWrapper<>();
        userTeamJoinQueryWrapper.in("teamId", teamIdList);
        //根据我加入的所有队伍的id在userTeam表中查询出所有的队伍用户信息
        List<UserTeam> userTeamList = userTeamService.list(userTeamJoinQueryWrapper);
        // 队伍 id => 加入这个队伍的用户列表

        //把我加入的所有队伍根据队伍id各自分组
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
       //todo 没看懂？
        teamList.forEach(team -> team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size()));


        return ResultUtils.success(teamList);
    }

    /**
     * 分页查询队伍
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        //把teamQuery的属性传递给此时的team对象，因为teamService里面带的curdApi是关于Team的
        BeanUtils.copyProperties(team, teamQuery);
        //定义分页信息（起始页数，页面大小）
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        //查询条件
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        //获取分页查询结果
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    //加入队伍
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    //退出队伍
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    //删除队伍
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null ||deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean result = teamService.deleteTeam(id, loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取我创建的队伍
     * @param
     * @param request
     * @return
     *
     * 备注：前端调用该接口不用传参
     * 原鱼皮代码
     */
//    @GetMapping("/list/my")
//    public BaseResponse<List<TeamUserVO>> listMyTeams(TeamQuery teamQuery,HttpServletRequest request){
//        if (teamQuery == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        //获取当前用户的登录信息
//        User loginUser = userService.getLoginUser(request);
//        teamQuery.setUserId(loginUser.getId());
//        //查询我创建的队伍，这里为了复用listTeams接口而把isAdmin参数设为true
//        List<TeamUserVO> teamList = teamService.listTeams(teamQuery,true);
//        return ResultUtils.success(teamList);
//    }

    /**
     * 获取我创建的队伍
     *
     * @param request
     * @return 备注：已优化
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyTeams(HttpServletRequest request) {
        TeamQuery teamQuery = new TeamQuery();
        //获取当前用户的登录信息
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        //查询我创建的队伍，这里为了复用listTeams接口而把isAdmin参数设为true
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);
        return ResultUtils.success(teamList);
    }

    /**
     * 获取我加入的队伍
     *
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamsList = userTeamService.list(queryWrapper);
        // 取出不重复的队伍 id
        // teamId userId
        // 1, 2
        // 1, 3
        // 2, 3
        // result
        // 1 => 2, 3
        // 2 => 3
        Map<Long, List<UserTeam>> listMap = userTeamsList.stream()
                .collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);

        //只有管理员才能查看加密还有非公开的房间
        List<TeamUserVO> teamList = teamService.listTeams(teamQuery, true);

        return ResultUtils.success(teamList);
    }



}
