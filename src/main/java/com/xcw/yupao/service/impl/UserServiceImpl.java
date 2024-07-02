package com.xcw.yupao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xcw.yupao.common.ErrorCode;
import com.xcw.yupao.exception.BusinessException;
import com.xcw.yupao.model.domain.User;
import com.xcw.yupao.mapper.UserMapper;

import com.xcw.yupao.service.UserService;
import com.xcw.yupao.utils.AlgorithmUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xcw.yupao.common.ErrorCode.*;
import static com.xcw.yupao.contant.UserConstant.ADMIN_ROLE;
import static com.xcw.yupao.contant.UserConstant.USER_LOGIN_STATE;



/**
 * @author xcw
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2024-04-23 16:18:32
 */
@Service
@Slf4j
@EnableScheduling
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";



    /**
     * 注册
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 校验密码
     * @param planetCode 星球编号
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.校验
        if (StringUtils.isAllBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(PARAMS_ERROR, "用户密码过短");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(PARAMS_ERROR, "星球编号过长");
        }

        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(PARAMS_ERROR);
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(PARAMS_ERROR);
        }

        //查询用户是否存在（账号重复性校验）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        //接受一个 QueryWrapper 对象作为参数，然后根据这个 QueryWrapper 构建的查询条件去数据库中统计符合条件的记录数量，并将这个数量返回。
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(PARAMS_ERROR, "账号重复");
        }

        //星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        //接受一个 QueryWrapper 对象作为参数，然后根据这个 QueryWrapper 构建的查询条件去数据库中统计符合条件的记录数量，并将这个数量返回。
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(PARAMS_ERROR, "编号重复");
        }


        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        //保存到数据库
        //关键字 this 指的是当前对象的引用，即表示当前类的一个实例对象，这里是userRegister类的实例
        boolean saveResult = this.save(user);

        if (!saveResult) {
            throw new BusinessException(PARAMS_ERROR, "保存用户数据失败");
        }
        //另一种mapper层的方法
//        int n = userMapper.insert(user);
//        if (n <=0) {
//
//            return -1;
//        }

        return user.getId();
    }

    //登录
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if (StringUtils.isAllBlank(userAccount, userPassword)) {
            throw new BusinessException(PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(PARAMS_ERROR, "账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(PARAMS_ERROR, "密码过短");
        }

        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        //根据条件查询单个对象,如果查询结果有多条记录，selectOne方法只会返回第一条记录。
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user == null) {
            log.info("用户登录失败，userAccount cannot match userPassword");
            throw new BusinessException(PARAMS_ERROR, "用户不存在或密码错误");
        }

        //3.用户脱敏

        User safetyUser = getSafetyUser(user);

        //4.记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }


    /**
     * @param orignUser
     * @return
     */
    @Override
    public User getSafetyUser(User orignUser) {
        if (orignUser == null) {
            return null;
        }
        //方法二：把脱敏的信息判空
//        User safetyUser =orignUser;
//        safetyUser.setUserPassword("");

        //方法一：把需要的对象属性赋值给safetyUser新对象并返回
        User safetyUser = new User();
        safetyUser.setId(orignUser.getId());
        safetyUser.setUsername(orignUser.getUsername());
        safetyUser.setUserAccount(orignUser.getUserAccount());
        safetyUser.setAvatarUrl(orignUser.getAvatarUrl());
        safetyUser.setGender(orignUser.getGender());
        safetyUser.setPhone(orignUser.getPhone());
        safetyUser.setEmail(orignUser.getEmail());
        safetyUser.setPlanetCode(orignUser.getPlanetCode());
        safetyUser.setUserRole(orignUser.getUserRole());
        safetyUser.setUserStatus(orignUser.getUserStatus());
        safetyUser.setCreateTime(orignUser.getCreateTime());
        safetyUser.setTags(orignUser.getTags());
        return safetyUser;
    }

    /**
     * 获取当前用户的登录信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // TODO: 2024/6/27 优化：用+登录拦截器+注解+切面的方式判断是否登录
        if (request == null) {
            return null;
        }
        //获取当前登录用户信息
        User userlogin = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userlogin == null) {
            //使用类名引用
            //throw new BusinessException(ErrorCode.NO_AUTH);
            //使用静态导入
            throw new BusinessException(NO_AUTH);
        }
        return userlogin;
    }

    @Override
    //验证是否为管理员
    public boolean isAdmin(HttpServletRequest request) {
        User attribute = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
//        if (attribute == null || attribute.getUserRole() != ADMIN_ROLE) {
//            return false;
//        }
//        return true; 简写如下
        return attribute != null && attribute.getUserRole() == ADMIN_ROLE;
    }

    @Override
    //验证是否为管理员
    public boolean isAdmin(User loginUser) {

        /*if (loginUser != null && loginUser.getUserrole() == ADMIN_ROLE)
        {return true;}
        return false;*/
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;

    }

    /**
     * 匹配用户
     * @param num 匹配人数
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 只选择id和tags字段
        queryWrapper.select("id", "tags");
        // 查询条件：tags字段不为空
        queryWrapper.isNotNull("tags");
        // 根据查询条件获取用户列表
        List<User> userList = this.list(queryWrapper);
        // 获取登录用户的标签
        String tags = loginUser.getTags();
        // 使用Gson将标签字符串转换为标签列表
        ////因为tag在数据库中是存取的一个String类型的json字符串，使用 Gson 实例将字符串 tag 反序列化为一个 List<String> 类型的对象。
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // / 用于存储每个用户及其与登录用户的相似度（编辑距离）
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 如果用户没有标签或用户是当前登录用户，则跳过
            if (StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())) {
                continue;
            }

            // 将用户的标签字符串转换为标签列表
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算登录用户标签列表与当前用户标签列表的编辑距离
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            // 将用户及其相似度加入列表
            list.add(new Pair<>(user, distance));
        }
        // 按相似度（编辑距离）从小到大排序，并取前num个用户
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表,获取前num个用户的ID列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // 根据ID列表查询用户信息
        userQueryWrapper.in("id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        // 按照查询结果的用户ID将用户分组
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        // 根据原始顺序重新排序用户
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }


    //用户注销
    @Override
    public int userLogout(HttpServletRequest request) {
        //判断是否登录,false代表如果当前请求没有关联的会话，则返回 null，不会创建新地会话。
        if (request.getSession(false)==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**/
    @Override
    public List<User> selectAll() {
        return userMapper.selectList(null);
    }


    //根据标用户的签查询用户

    /**
     * 内存过滤 查询根据标签
     * filter方法对这些加载到内存中的用户数据进行过滤。
     * <p>
     * 在内存过滤查询中，首先执行一个查询以获取所有用户数据，然后将这些数据加载到应用程序的内存中。
     * 过滤条件是在应用程序层面上，使用Java代码和Stream API来实现的。
     * 这意味着所有的数据首先被加载到内存中，然后应用程序再进行过滤操作，这可能会消耗更多的内存和处理时间。
     *
     * @param tagNameList 标签列
     * @return
     */
//    @Override
//    public List<User> searchUserByTags(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(PARAMS_ERROR);
//        }
//
//        //第二种方法（内存过滤查询）
//        //1.先查询所有用户
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        List<User> userList = userMapper.selectList(queryWrapper);
//        Gson gson = new Gson();
//        //2.在内存中判断是否包含要求的标签
//        return userList.stream().filter(user -> {
//            //获得标签
//            String tagsStr = user.getTags();
//            //todo
////            if (StringUtils.isBlank(tagsStr)) {
////                return false;
////            }
//            //用set不用list的原因1.唯一性：Set 集合不允许有重复的元素，而 List 可以包含重复的元素。
//            //性能：在 Set 中查找元素（检查一个元素是否存在于集合中）通常比在 List 中更高效，因为 Set 通常是基于哈希表实现的，具有常数时间复杂度的查找性能（O(1)），
//            //而 List 是基于数组或链表实现的，查找性能为线性时间复杂度（O(n)）。
//            //反序列化:fromJson()：将json字符串转化为Java对象
//            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {}.getType());
//
//            //避免空指针异常（避免tempTagNameSet不为空和null）
//            /**
//             * Optional.ofNullable(tempTagNameSet)：尝试将 tempTagNameSet 包装成一个 Optional 对象。
//             * 、如果 tempTagNameSet 是 null，那么它将返回一个空的 Optional 对象；
//             * 、如果 tempTagNameSet 不是 null，那么它将返回一个包含 tempTagNameSet 的 Optional 对象
//             *
//             orElse(new HashSet<>())：如果 Optional 对象是空的（即 tempTagNameSet 为 null），
//             那么 orElse() 方法将返回 new HashSet<>()，
//             、也就是一个新的空 HashSet 实例。如果 Optional 对象不是空的，
//             、那么 orElse() 方法将返回 Optional 对象中的值，即 tempTagNameSet 本身。
//             */
//            tempTagNameSet= Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
//
//            //return false;和return true;是返回给filter操作的，它们决定了哪些用户将被包含在最终的列表中
//            for (String tagName : tagNameList){
//                if (!tempTagNameSet.contains(tagName)){
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
//    }
   /* @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        long startTime = System.currentTimeMillis();//

        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //第二种方法（内存过滤查询）
        //1.先查询所有用户

        long queryStartTime = System.currentTimeMillis();//

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);

        long queryEndTime = System.currentTimeMillis();//
        System.out.println("查询所有用户所花费的时间: " + (queryEndTime - queryStartTime) + " 毫秒");//

        Gson gson = new Gson();
        //2.在内存中判断是否包含要求的标签
        long filterStartTime = System.currentTimeMillis();//

        List<User> users01 = userList.stream().filter(user -> {
            //获得标签
            String tagsStr = user.getTags();
            //todo
//            if (StringUtils.isBlank(tagsStr)) {
//                return false;
//            }
            //反序列化:fromJson()：将json字符串转化为Java对象
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

        System.out.println("424行：查询所有用户所花费的时间: " + (queryEndTime - queryStartTime) + " 毫秒");//

        long filterEndTime = System.currentTimeMillis();
        System.out.println("在内存中过滤用户所花费的时间: " + (filterEndTime - filterStartTime) + " 毫秒");


        long endTime = System.currentTimeMillis();
        System.out.println("根据标签查询用户的总时间: " + (endTime - startTime) + " 毫秒");

        return users01;
    }*/
    /**
     * 修改用户信息
     * 注意id是数据库生的,固定死的
     *
     * @param user      最新的用户信息（前端传过来要修改的用户信息,）
     * @param loginUser 目前登录的用户
     * @return 返回更新操作影响的行数
     */
    @Override
    public int updateUser(User user, User loginUser) {

        if (loginUser == null) {
            throw new BusinessException(PARAMS_ERROR);
        }

        //获取要修改的用户id，并判断输入的id是否合理
        long userId = user.getId();
        if (userId < 0) {
            throw new BusinessException(PARAMS_ERROR);
        }

        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(NO_AUTH);
        }

        //根据id 查询要修改的用户的是否存在
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(NULL_ERROR);
        }

        //跟新用户信息并返回更新操作影响的行数
        return userMapper.updateById(user);
    }


    /**
     *   根据标签搜索用户。
     * @param tagNameList  用户要搜索的标签
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList){
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return sqlSearch(tagNameList);   //先 sql query time = 579ms 后 memory query time = 5606
        //return memorySearch(tagNameList);    // 先 memory query time = 5938 后 sql query time = 5956 （清过缓存）
    }



    /**
     * 第一种方法
     * sql查询 根据标签
     * 由于SQL查询是在数据库中执行的，所以它通常更高效，特别是在处理大量数据时，因为数据库可以利用索引来优化查询。
     *
     * @param tagNameList
     * @return
     */
    //@Deprecated//表示该方法过期不用]
    public List<User> sqlSearch(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(PARAMS_ERROR);
        }

        //第一种方法
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        //forEach 方法是用来对集合中的每个元素执行一个操作，但它并不返回任何值。
        //forEach 方法的返回类型是 void
        //return userList.forEach(this::getSafetyUser);
        //stream流处理list集合（获取流→流的中间方法→流的终结方法）
        //简化return userList.stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }


    /**
     * 第二种方法
     * 查询，内存运行筛选
     * @param tagNameList
     * @return
     */
    public List<User> memorySearch(List<String> tagNameList){

        //1.先查询所有用户
        QueryWrapper queryWrapper = new QueryWrapper<>();
        //long starTime = System.currentTimeMillis();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2.判断内存中是否包含要求的标签
        userList.stream().filter(user -> {
            String tagstr = user.getTags();
            if (StringUtils.isBlank(tagstr)){
                return false;
            }
            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
            for (String tagName : tagNameList){
                if (!tempTagNameSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        //log.info("memory query time = " + (System.currentTimeMillis() - starTime));
        return  userList;
    }


}




