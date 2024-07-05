package com.xcw.yupao.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.model.request.UserRegisterRequest;
import com.xcw.yupao.model.vo.UserVO;


import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xcw
* @description 针对表【user】的数据库操作Service
* @createDate 2024-04-23 16:18:32
*/
public interface UserService extends IService<User> {


    /**
     * 注册
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return 新用户id
     */
     //long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request 请求
     * @return 返回脱敏后的账户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     *用户脱敏
     * @param orignUser 原始的用户
     * @return 脱敏的用户
     */
    User getSafetyUser(User orignUser);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    List<User> selectAll();


    /**
     * 根据标签查询 内存中查询
     * @param tagNameList 标签列
     * @return 用户
     */
    List<User> searchUserByTags(List<String> tagNameList);

    /**
     * 修改用户信息
     * @param user 要修改的用户
     * @param loginUser 登录的用户
     * @return
     */
    int updateUser(User user,User loginUser);

    /**
     * 获取当前登录用户的信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 判断是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    //验证是否为管理员
    boolean isAdmin(User loginUser);

    /**
     * 匹配用户
     * @param num
     * @param loginUser
     */
    List<User> matchUsers(long num, User loginUser);

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 修改用户的标签
     * @param userId
     * @param loginUser
     * @param tags
     * @return
     */
    int updateTags(Long userId, User loginUser, List<String> tags);
}
