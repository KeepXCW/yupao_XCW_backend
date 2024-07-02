package com.xcw.yupao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xcw.yupao.common.BaseResponse;
import com.xcw.yupao.common.ErrorCode;
import com.xcw.yupao.common.ResultUtils;
import com.xcw.yupao.exception.BusinessException;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.model.request.UserLoginRequest;
import com.xcw.yupao.model.request.UserRegisterRequest;
import com.xcw.yupao.model.vo.UserVO;
import com.xcw.yupao.service.UserService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xcw.yupao.common.ErrorCode.PARAMS_ERROR;
import static com.xcw.yupao.contant.UserConstant.USER_LOGIN_STATE;


/**
 * 此类用于编写RESTful风格的API，并使返回值默认为JSON类型。
 * 该注解是Spring框架提供，用于标记类作为控制器，
 * 其中所有的方法都映射为HTTP请求的处理方法。
 */
@RestController
@Api(tags = "用户")//定义knife4j文档中这个类接口目录名字
@RequestMapping("/user")
/**
 * allowCredentials = "true" 是 @CrossOrigin 注解中的一个属性，
 * 它的作用是允许浏览器发送和接收跨域请求时携带凭证信息（例如Cookies、HTTP认证头或TLS客户端证书）。
 */
@CrossOrigin(origins = {"http://localhost:3000"}, allowCredentials = "true")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    //redis中data.redis包里自带的操控redis增删改查的类(如果使用的不是springboot则需要自定义一个这样的类)
    private RedisTemplate<String, Page<User>> redisTemplate;

    //注册
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {

        //@RequestBody把前端的json参数和这里的参数关联

        if (userRegisterRequest == null) {
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(PARAMS_ERROR);
        }

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        //Apache Commons Lang 库中的一个方法，它用于检查传入的所有字符串参数是否全部为空或者空白,返回布尔值
        if (StringUtils.isAllBlank(userAccount, userPassword, checkPassword, planetCode)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        //return new BaseResponse<>(0, result, "ok");

        return ResultUtils.success(result);
    }

    //登录
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        //@RequestBody把前端的json参数和这里的参数关联


        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        //Apache Commons Lang 库中的一个方法，它用于检查传入的所有字符串参数是否全部为空或者空白,返回布尔值
        if (StringUtils.isAllBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);

        //return new BaseResponse<>(0, user, "ok");
        return ResultUtils.success(user);
    }

    //退出登录
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {

        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    //获取当前用户的登录状态
    //todo 最后优化调用service层的getLoginUser接口实线
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) attribute;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //从session中取出当前登录的用户的id
        long userId = currentUser.getId();
        //todo 校验用户是否合法

        //根据id从数据库查询当前用户的信息
        User user = userService.getById(userId);
        //返回脱敏后的用户信息
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 推荐页面 分页展示
     * mybatisplus的方式(不用引入任何依赖)
     *
     * @param request
     * @return
     */
    // TODO: 2024/6/28 推荐多个未实现
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageNum, long pageSize, HttpServletRequest request) {

        //通过HttpServletRequest对象request 获取当前登录的用户
        User loginUser = userService.getLoginUser(request);
        //用JAVA字符串格式化，创建了一个用于缓存的键，格式为yupao:user:recommend:<用户ID>。
        /**redisKey设置的一般格式
         systemId:moduleId:func:options（不要和别人冲突）
         yupao:user:recommed:userId**/
        String redisKey = String.format("yupao:user:recommend:%s", loginUser.getId());

        //第二次查询从内存中查询
        /*opsForValue()方法提供了一个便捷的方式来获取操作Redis字符串值的接口实例
        opsForValue()方法提供的是操作Redis中字符串类型值（value）的接口实例,里面有增删改查等方法*/
        ValueOperations<String, Page<User>> valueOperations = redisTemplate.opsForValue();
        //查询：根据主键查询（如果有缓存，直接读缓存）
        Page<User> userPage = valueOperations.get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }

        //无缓存(第一次查询)从数据库查
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);

        //把查询结果写入缓存过期时间为30秒，TimeUnit.MILLISECONDS指定了 30000 这个数字的时间单位是毫秒
        try {
            //edis 内存不能无限增加，一定要设置过期时间！！！
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtils.success(userPage);
    }


    //查询：根据用户名查询
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        //校验是否为管理员
        if (!userService.isAdmin(request)) {
            //return new ArrayList<>();
            //return ResultUtils.success(new ArrayList<>());
            throw new BusinessException(ErrorCode.NO_AUTH, "非管理员无权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        //username没有空格且不等于空
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }

        List<User> userlist = userService.list(queryWrapper);
        List<User> list = userlist.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    //根据标签查询用户
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.success(userList);
    }



    //删除用户
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        if (id <= 0) {
            throw new BusinessException(PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }


    @GetMapping("/selectAll")
    public List<User> selectAll() {
        return userService.selectAll();
    }

    /**
     * 更新用户信息
     *
     * @param user    前端传过来的要更新的json用户信息
     * @param request 用来获取当前登录的用户信息
     * @return int 受影响的行数
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        //校验参数是否为空
        if (user == null) {
            throw new BusinessException(PARAMS_ERROR, "查询用户输入为空");
        }
        //获取当前用户的登录信息
        User loginUser = userService.getLoginUser(request);
        //
        int result = userService.updateUser(user, loginUser);

        return ResultUtils.success(result);
    }
    /**
     * 获取最匹配的用户
     *
     * @param num 匹配的数量
     * @param request 用来获取当前登录的用户信息
     * @return  List<UserVO>
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, user));
    }

}
