package com.xcw.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.yupao.common.ErrorCode;
import com.xcw.yupao.exception.BusinessException;
import com.xcw.yupao.model.domain.Team;
import com.xcw.yupao.mapper.TeamMapper;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.model.domain.UserTeam;
import com.xcw.yupao.model.dto.TeamQuery;
import com.xcw.yupao.model.enums.TeamStatusEnum;
import com.xcw.yupao.model.request.TeamJoinRequest;
import com.xcw.yupao.model.request.TeamQuitRequest;
import com.xcw.yupao.model.request.TeamUpdateRequest;
import com.xcw.yupao.model.vo.TeamUserVO;
import com.xcw.yupao.model.vo.UserVO;
import com.xcw.yupao.service.TeamService;
import com.xcw.yupao.service.UserService;
import com.xcw.yupao.service.UserTeamService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author xcw
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2024-06-11 20:59:20
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {


    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 添加队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    @Override
    /**事务，sql失败回滚*/
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(@RequestBody Team team, User loginUser) {
        //1.请求参数是否为空

        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //把teamAddRequest属性赋值给team对象
        /*Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);*/
        //2.是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        //获取当前登录用户的id
        final long userId = loginUser.getId();

        //3.检验信息
        //(1).队伍人数>=1且<=20

        //如果为空，直接赋值为0,因为MaxNum为包装类Integer修饰，为null时要赋默认值0，便于下面比较
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
        //(2).队伍名字 <=20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
        //(3). 描述<= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //(4).status 是否公开，不传默认为0
        //如果为空，直接赋值为0,因为MaxNum为包装类Integer修饰，为null时要赋默认值0，便于下面比较
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        //判断用户输入队伍的状态（0/1/2）属性status是否合法存在,存在就返回status
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
        //(5).如果status是加密状态，一定要密码 且密码<=32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
            }
        }
        //(6).超出时间 > 当前时间
        // TODO: 2024/6/27 优化过期时间输入格式(不行把时去掉)
        //当前时间
        Date expireTime = team.getExpireTime();
        //如果比当前时间慢
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超出时间 > 当前时间");
        }
        //(7).校验用户最多创建5个队伍
        //todo 有bug。可能同时创建100个队伍,要用锁处理防止并发
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //当前用户的id作为查询条件
        QueryWrapper<Team> id = queryWrapper.eq("userId", userId);
        long count = this.count(id);
        //注意：因为这里是在用户创建队伍后才查询队伍的数量，所以判断条件要考虑=5
        if (count >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
        }

        //4.插入队伍信息到队伍表
        //队伍创建人id，默认第一个用户id也就是队长id
        //team.setCreatUserId(userId);//xcw加

        team.setId(null);
        //队伍队长id
        team.setUserId(userId);

        boolean result = this.save(team);
        //获取刚创建的队伍的id
        Long teamId = team.getId();

        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }


        //5.插入用户 ==> 队伍关系 到关系表（中间表）
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        //这里result和上面的result是一个目的是
        //todo 测试下面不是result变量 /0异常 事务回滚是否触发
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        //方法报返回值可能npe，已校验不用管
        return teamId;
    }


    /**
     * 查询队伍及关联查询加入的成员信息
     *
     * @param teamQuery 脱敏队伍信息实体类
     * @return
     */
    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //组合查询条件
        //查询条件为空则查询出所有队伍的id
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            //1.根据队伍id查询
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            //搜索我加入的队伍
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                //在集合中遍历id查询队伍
                queryWrapper.in("id", idList);
            }
            //2.搜索关键词（同时对队伍名称和描述搜索）
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                //多条件查询（and里面是lambda表达式）
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            //根据队伍名字查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            //根据描述查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            //根据队伍最大人数查询
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }


            Long userId = teamQuery.getUserId();

            //根据创建人id来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }

            //根据状态来查询
            Integer status = teamQuery.getStatus();
            //通过getEnumByValu()判断teamQuery中的status在队伍状态枚举类中是否存在
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            //当查询请求teamQuery中，未设置status时，默认status为公开的
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            //只有管理员才能查看加密还有非公开的房间
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }


        //不展示已过期的队伍, gt是 "greater than" 的缩写，即 "大于"。
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        //多条件查询出队伍信息
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        List<TeamUserVO> teamUserVOList = new ArrayList<>();

        //关联查询创建人的用户信息
        //todo 优化自己写SQL、减少性能消耗
        for (Team team : teamList) {
            //获取teamList集合里面每一个team的创建人id
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            //根据id查询出队伍创建人
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            //脱敏队伍信息 （把team的属性传递给teamUserVO脱敏要返回给前端的信息）
            BeanUtils.copyProperties(team, teamUserVO);
            //脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    /**
     * 分页查询队伍及关联查询加入的成员信息
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public Page<TeamUserVO> listTeamsBypage(TeamQuery teamQuery, boolean isAdmin) {

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();

        //组合查询条件
        //查询条件为空则查询出所有队伍的id
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            //1.根据队伍id查询
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            //搜索我加入的队伍
            List<Long> idList = teamQuery.getIdList();
            if (CollectionUtils.isNotEmpty(idList)) {
                //在集合中遍历id查询队伍
                queryWrapper.in("id", idList);
            }
            //2.搜索关键词（同时对队伍名称和描述搜索）
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                //多条件查询（and里面是lambda表达式）
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            //根据队伍名字查询
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            //根据描述查询
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            //根据队伍最大人数查询
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }


            Long userId = teamQuery.getUserId();

            //根据创建人id来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }

            //根据状态来查询
            Integer status = teamQuery.getStatus();
            //通过getEnumByValu()判断teamQuery中的status在队伍状态枚举类中是否存在
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            //当查询请求teamQuery中，未设置status时，默认status为公开的
            if (statusEnum == null) {
                statusEnum = TeamStatusEnum.PUBLIC;
            }
            //只有管理员才能查看加密还有非公开的房间
            if (!isAdmin && statusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
            }
            queryWrapper.eq("status", statusEnum.getValue());
        }

        //不展示已过期的队伍, gt是 "greater than" 的缩写，即 "大于"。
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));

        //多条件分页查询出队伍信息  todo 注释不准确
        //List<Team> teamList = this.list(queryWrapper);
        Page<Team> teamListBypage = this.page(new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize()), queryWrapper);
        //获取队伍信息 todo 注释不准确
        List<Team> teamList = teamListBypage.getRecords();
        //判断teamListBypage为空则抛异常
        if (CollectionUtils.isEmpty(teamList)) {
            //返回空页面
            // todo 写的对吗？
            return new Page<>();
        }


        //关联查询创建人的用户信息,把Team 对象转换为 TeamUserVO 对象
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        //todo 优化自己写SQL、减少性能消耗
        for (Team team : teamList) {
            //获取teamList集合里面每一个team的创建人id
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            //根据id查询出队伍创建人
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            //脱敏队伍信息 （把teamList里的每一个team的属性传递给teamUserVO脱敏要返回给前端的信息）
            BeanUtils.copyProperties(team, teamUserVO);

            //脱敏用户信息
            if (user != null) {
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        //改动：返回分页查询到的teamUserVOList，保留分页信息
        //改动原因：返回正确的分页对象，包含记录列表和分页信息
        Page<TeamUserVO> resultPage = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize(), teamListBypage.getTotal());
        //把数据存入列表Records
        resultPage.setRecords(teamUserVOList);
        return resultPage;
    }

    /**
     *查询私有队伍
     * @param teamQuery
     * @return
     */
    @Override
    public List<TeamUserVO> listPrivateTeams(TeamQuery teamQuery, HttpServletRequest request) {
        if (teamQuery == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        User loginUser = userService.getLoginUser(request);
        //不是管理员或者队伍创建本人不允许查询私有房间
        if (!isAdmin && teamQuery.getUserId() != null && !teamQuery.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }

        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //status=1就是私有队伍
        queryWrapper.eq("status", 1);
        List<Team> teamList = this.list(queryWrapper);
        ArrayList<TeamUserVO> teamUserVOS = new ArrayList<>();
        for (Team team : teamList) {
            Long teamId = team.getId();
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //查关联表，获取加入队伍的人数
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("teamId", teamId);
            long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
            teamUserVO.setHasJoinNum((int) hasJoinNum);
            teamUserVOS.add(teamUserVO);
        }
        return teamUserVOS;
    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //查询要修改的队伍信息
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者队伍的创建者可以修改
        //实体类没有重写equals所以也只是比较对象的值而不比较引用地址，鱼皮的User类id用的long所以是！=
        if (!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        //加密房间必须设置密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
        }

        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    /**
     * 用户加入队伍
     *
     * @param teamJoinRequest
     * @param loginUser
     * @return Todo 加锁避免用户疯狂点击重复加入
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //从teamJoinRequest中获取封装的队伍id属性
        Long teamId = teamJoinRequest.getTeamId();
        //根据队伍id查询要加入的队伍
        Team team = getTeamById(teamId);

        //获取队伍的过期时间
        Date expireTime = team.getExpireTime();
        //不能加入已经过期的队伍
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        //获取队伍状态
        Integer status = team.getStatus();
        //todo 从队伍状态枚举类中取，保证其存在(创建队伍时已校验过,也可不写)
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        //用equals时最好把非空的数据放外边,equals里面放不确定的数据
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码有误，加入队伍失败");
            }
        }


        //已加入和创建的队伍数<5——根据当前登录的用户id在Userteam表中关联查询加入和创建的队伍数，数量<5
        /*Long userId = loginUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        //查询已创建的队伍数
        long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinNum > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入5个队伍");
        }
        //不能重复加入同一队伍——在用户队伍关系表中查询要加入的队伍id和当前用户id存在的数量
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        //查询是否已加入当前队伍
        long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
        //hasUserJoinTeam 一般是等于 1,因为同一个队伍，用户只能同时在里面存在一个
        if (hasUserJoinTeam > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        //不能加入满员的队伍人数——查询已加入的队伍人数,与maxNUm比较
        //todo 优化
//        QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
//        teamQueryWrapper.eq("teamId", teamId);
//        long teamHasJoinNUm = userTeamService.count(teamQueryWrapper);
       *//* userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);*//*


        long teamHasJoinNUm = this.countTeamUserByTeamId(teamId);
        //long teamHasJoinNUm = userTeamService.count(userTeamQueryWrapper);
        if (teamHasJoinNUm >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
        }

        // 修改队伍信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        //插入成功失败返回布尔值
        return userTeamService.save(userTeam);*/

        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long teamId = teamQuitRequest.getTeamId();
        //根据id获取队伍信息
        Team team = getTeamById(teamId);
        //获取当前登录用户（也是要退出的用户）id
        long userId = loginUser.getId();
        //QueryWrapper条件查询两种写法
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        //查询用户已经加入的队伍数
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(queryUserTeam);
        /* 常见写法二：userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);*/
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }

        //查询teamId的队伍中有几人
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        //队伍只剩一人，队伍解散
        if (teamHasJoinNum == 1) {
            //删除队伍和用户队伍关系表中的相关数据

            //1.删除队伍表中数据
           /*注意这里删除队伍的时候,最好不要调用同类中的deleteTeam(long id, User loginUser)方法,原因如下：
           1.此方法和deleteTeam()方法都执行了重复的查询操作，浪费数据库性能
           2.事务的传播行为：这里的事务都是默认的REQUIRED，此方法中调用的deleteTeam的事务不会生效,需要在调用的方法上配置REQUIRES_NEW
           才能是的调用的方法执行时单独开启一个新事务
            */
            this.removeById(teamId);
        } else {
            //2.删除用户队伍关系表中数据
            //是否是队长
            //是队长——更新队长
            if (team.getUserId() == userId) {
                //把队长转移给最早加入的用户
                //1.查询已经加入队伍的所有用户和加入的时间
                QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                userTeamQueryWrapper.eq("teamId", teamId);
                //在查询语句后面拼接sql语句（只查询前两条id的userTeam数据）
                userTeamQueryWrapper.last("order by id asc limit 2");
                //查询出队伍中id考前的前两个用户，把第二个用户更新成队长
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam nextUserTeam = userTeamList.get(1);
                Long nextTeamLeaderId = nextUserTeam.getUserId();
                //更新当前队伍队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        //移除关系,queryWrapper为(UserTeam中userId和teamId，即当前用户加入的队伍
        return userTeamService.remove(queryWrapper);
    }

    /**
     * 删除(解散)队伍
     *
     * @param id
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//多个数据库操作(增删改)要用事务回滚
    public boolean deleteTeam(long id, User loginUser) {
        // 根据id校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        // 校验你是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }


    /**
     * 根据id获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null && teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //根据teamQuitRequest请求取到的要退出队伍的id,获取队伍对象
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        //用户队伍表中有几个相同的teamId，就代表teamId对应的该队伍有几个加入的成员,看表结构可知
        return userTeamService.count(userTeamQueryWrapper);
    }

}





