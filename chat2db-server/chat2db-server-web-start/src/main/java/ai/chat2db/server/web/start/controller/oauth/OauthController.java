package ai.chat2db.server.web.start.controller.oauth;

import ai.chat2db.server.domain.api.enums.RoleCodeEnum;
import ai.chat2db.server.domain.api.enums.ValidStatusEnum;
import ai.chat2db.server.domain.api.model.User;
import ai.chat2db.server.domain.api.param.user.UserUpdateParam;
import ai.chat2db.server.domain.api.service.UserService;
import ai.chat2db.server.web.start.controller.oauth.request.ChangeUserAndPwdRequest;
import ai.chat2db.server.web.start.controller.oauth.request.LoginRequest;
import ai.chat2db.server.tools.base.excption.BusinessException;
import ai.chat2db.server.tools.base.wrapper.result.ActionResult;
import ai.chat2db.server.tools.base.wrapper.result.DataResult;
import ai.chat2db.server.tools.common.model.LoginUser;
import ai.chat2db.server.tools.common.util.ContextUtils;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Login authorization service
 *
 * @author Jiaju Zhuang
 */
@RestController
@RequestMapping("/api/oauth")
@Slf4j
public class OauthController {

    @Resource
    private UserService userService;

    /**
     * 修改用户名及密码
     * @param request
     * @return
     */
    @PostMapping("changUserAndPwd")
    public DataResult changeUserAndPwd(@Validated @RequestBody ChangeUserAndPwdRequest request) {
        //   Query user
        User user = userService.query(request.getUserId()).getData();
        this.validateUser(user);
        UserUpdateParam updateParam = new UserUpdateParam();
        updateParam.setId(user.getId());
        updateParam.setNickName(request.getUserName());
        updateParam.setPassword(request.getPassword());
        updateParam.setStatus(user.getStatus());
        updateParam.setEmail(user.getEmail());
        updateParam.setRoleCode(user.getRoleCode());
        userService.update(updateParam);
        return DataResult.of(getLoginUser());
    }


    /**
     * Login with username and password
     *
     * @param request
     * @return
     */
    @PostMapping("login_a")
    public DataResult login(@Validated @RequestBody LoginRequest request) {
        //   Query user
        User user = userService.query(request.getUserName()).getData();
        this.validateUser(user);

        // Successfully logged in without modifying the administrator password
        if (this.validateAdmin(user)) {
            return DataResult.of(doLogin(user));
        }

        if (!DigestUtil.bcryptCheck(request.getPassword(), user.getPassword())) {
            throw new BusinessException("oauth.passwordIncorrect");
        }

        return DataResult.of(doLogin(user));
    }

    private boolean validateAdmin(final @NotNull User user) {
        return RoleCodeEnum.ADMIN.getDefaultUserId().equals(user.getId()) && RoleCodeEnum.ADMIN.getPassword().equals(
                user.getPassword());
    }

    private void validateUser(final User user) {
        if (Objects.isNull(user)) {
            throw new BusinessException("oauth.userNameNotExits");
        }
        if (!ValidStatusEnum.VALID.getCode().equals(user.getStatus())) {
            throw new BusinessException("oauth.invalidUserName");
        }
        if (RoleCodeEnum.DESKTOP.getDefaultUserId().equals(user.getId())) {
            throw new BusinessException("oauth.IllegalUserName");
        }
    }

    private Object doLogin(User user) {
        StpUtil.login(user.getId());
        return SaHolder.getStorage().get(SaTokenConsts.JUST_CREATED_NOT_PREFIX);
    }

    /**
     * Sign out
     *
     * @return
     */
    @PostMapping("logout_a")
    public ActionResult logout() {
        StpUtil.logout();
        return ActionResult.isSuccess();
    }

    /**
     * user
     *
     * @return
     */
    @GetMapping("user")
    public DataResult<LoginUser> user() {
        return DataResult.of(getLoginUser());
    }

    /**
     * user
     *
     * @return
     */
    @GetMapping("user_a")
    public DataResult<LoginUser> usera() {
        return DataResult.of(getLoginUser());
    }

    private LoginUser getLoginUser() {
        return ContextUtils.queryLoginUser();
    }

}
