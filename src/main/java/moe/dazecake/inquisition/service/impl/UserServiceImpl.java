package moe.dazecake.inquisition.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.CreateQrcodeReq;
import moe.dazecake.inquisition.mapper.AccountMapper;
import moe.dazecake.inquisition.mapper.BillMapper;
import moe.dazecake.inquisition.mapper.ProUserMapper;
import moe.dazecake.inquisition.mapper.mapstruct.AccountConvert;
import moe.dazecake.inquisition.model.dto.account.AccountDTO;
import moe.dazecake.inquisition.model.dto.log.LogDTO;
import moe.dazecake.inquisition.model.dto.user.CreateUserByPayDTO;
import moe.dazecake.inquisition.model.dto.user.UserStatusSTO;
import moe.dazecake.inquisition.model.entity.AccountEntity;
import moe.dazecake.inquisition.model.entity.TaskDateSet.LockTask;
import moe.dazecake.inquisition.model.vo.UserLoginVO;
import moe.dazecake.inquisition.model.vo.query.PageQueryVO;
import moe.dazecake.inquisition.service.intf.UserService;
import moe.dazecake.inquisition.utils.DynamicInfo;
import moe.dazecake.inquisition.utils.JWTUtils;
import moe.dazecake.inquisition.utils.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    DynamicInfo dynamicInfo;

    @Resource
    AccountMapper accountMapper;

    @Resource
    LogServiceImpl logService;

    @Resource
    HttpServiceImpl httpService;

    @Resource
    CDKServiceImpl cdkService;

    @Resource
    PayServiceImpl payService;

    @Resource
    TaskServiceImpl taskService;

    @Resource
    AccountServiceImpl accountService;

    @Resource
    BillMapper billMapper;

    @Resource
    ProUserMapper proUserMapper;

    @Value("${wx-pusher.app-token:}")
    String appToken;

    @Value("${wx-pusher.enable:false}")
    boolean enableWxPusher;

    @Override
    public Result<String> createUserByCDK(String cdk, String username, String account, String password, Integer server) {
        if (accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                .eq(AccountEntity::getAccount, account)).size() != 0) {
            return Result.forbidden("?????????????????????????????????");
        }

        var newAccount = new AccountEntity();
        newAccount.setName(username)
                .setAccount(account)
                .setPassword(password);
        return cdkService.createUserByCDK(newAccount, cdk);
    }

    @Override
    public Result<String> createUserByPay(CreateUserByPayDTO createUserByPayDTO, String username, String account, String password, Integer server) {
        if (username.contains("|") || account.contains("|") || password.contains("|")) {
            return Result.paramError("?????????????????????????????????????????? | ??????");
        }
        var proUser = proUserMapper.selectById(createUserByPayDTO.getAgent());
        if (proUser == null) {
            return Result.notFound("??????????????????");
        }

        if (accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                .eq(AccountEntity::getAccount, account)).size() != 0) {
            return Result.forbidden("?????????????????????????????????");
        }

        var bill = payService.createOrder(1.0, createUserByPayDTO.getPayType(), "/auth/user/");
        bill.setType("register")
                .setParam(username + "|" + account + "|" + password + "|" + server);
        if (createUserByPayDTO.getAgent() != 0) {
            bill.setParam(bill.getParam() + "|" + createUserByPayDTO.getAgent());
        } else {
            bill.setParam(bill.getParam() + "|0");
        }
        billMapper.updateById(bill);

        return Result.success(bill.getPayUrl(), "?????????????????????????????????");
    }

    @Override
    public Result<UserLoginVO> userLogin(String account, String password) {
        if (account == null || password == null) {
            return Result.unauthorized("?????????????????????");
        }

        var user = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getAccount, account)
                        .eq(AccountEntity::getPassword, password)
        );

        if (user != null) {
            if (user.getDelete() == 1) {
                return Result.forbidden("?????????????????????????????????????????????");
            }
            return Result.success(new UserLoginVO(JWTUtils.generateTokenForUser(user)), "????????????");
        } else {
            return Result.notFound("??????????????????");
        }
    }

    @Override
    public Result<AccountDTO> showMyAccount(Long id) {
        return Result.success(AccountConvert.INSTANCE.toAccountDTO(accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        )), "????????????");
    }

    @Override
    public Result<String> updateMyAccount(Long id, AccountDTO accountDTO) {
        var newAccount = AccountConvert.INSTANCE.toAccountEntity(accountDTO);
        var oldAccount = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        );
        if (oldAccount != null) {

            oldAccount.setName(newAccount.getName())
                    .setConfig(newAccount.getConfig())
                    .setActive(newAccount.getActive())
                    .setNotice(newAccount.getNotice());
            accountMapper.updateById(oldAccount);

            return Result.success("????????????");

        } else {
            return Result.notFound("??????????????????");
        }
    }

    @Override
    public Result<String> updateAccountAndPassword(Long id, String account, String password, Long server) {

        if (account.isBlank() || password.isBlank() || server == null) {
            return Result.paramError("???????????????");
        }

        var accountList = accountMapper.selectList(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getAccount, account)
        );
        if (accountList.size() > 1) {
            return Result.forbidden("????????????????????????");
        }

        var accountEntity = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        );

        if (server == 0) {
            if (httpService.isOfficialAccountWork(account, password)) {
                accountEntity.setAccount(account);
                accountEntity.setPassword(password);
                accountEntity.setServer(server);
                accountEntity.setUpdateTime(LocalDateTime.now());
                accountMapper.updateById(accountEntity);
            } else {
                return Result.unauthorized("????????????????????????????????????");
            }
        } else if (server == 1) {
            if (httpService.isBiliAccountWork(account, password)) {
                accountEntity.setAccount(account);
                accountEntity.setPassword(password);
                accountEntity.setServer(server);
                accountEntity.setFreeze(0);
                accountEntity.getBLimitDevice().clear();
                accountEntity.setUpdateTime(LocalDateTime.now());
                accountMapper.updateById(accountEntity);
            } else {
                return Result.unauthorized("????????????????????????????????????");
            }
        }

        return Result.success("????????????");
    }

    @Override
    public Result<String> freezeMyAccount(Long id) {
        var account = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        );
        if (account != null) {
            account.setFreeze(1);
            accountMapper.updateById(account);
            dynamicInfo.getUserSanList().remove(account.getId());
            return Result.success("????????????");
        } else {
            return Result.notFound("??????????????????");
        }
    }

    @Override
    public Result<String> unfreezeMyAccount(Long id) {
        var account = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        );
        if (account != null) {
            account.setFreeze(0);
            accountMapper.updateById(account);
            if (dynamicInfo.getUserMaxSanList().containsKey(account.getId())) {
                dynamicInfo.getUserSanList().put(account.getId(),
                        dynamicInfo.getUserMaxSanList().get(account.getId()) - 20);
            } else {
                dynamicInfo.getUserSanList().put(account.getId(), 80);
                dynamicInfo.getUserMaxSanList().put(account.getId(), 100);
            }
            return Result.success("????????????");
        } else {
            return Result.notFound("??????????????????");
        }
    }

    @Override
    public Result<PageQueryVO<LogDTO>> showMyLog(String account, Long current, Long size) {
        return Result.success(logService.queryLogByAccount(account, current, size), "????????????");
    }

    @Override
    public Result<UserStatusSTO> showMyStatus(Long id) {
        var user = accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getId, id)
        );
        if (user == null) {
            return Result.notFound("??????????????????");
        }
        if (user.getFreeze() == 1) {
            return Result.success(new UserStatusSTO("???????????????????????????????????????????????????"), "????????????");
        }

        for (Long k : dynamicInfo.getFreezeTaskList().keySet()) {
            if (Objects.equals(k, id)) {
                return Result.success(new UserStatusSTO("????????????????????????????????????????????????????????????"), "????????????");
            }
        }

        var index = 0;
        for (AccountEntity account : dynamicInfo.getFreeTaskList()) {
            if (account.getId().equals(id)) {
                if (index != 0) {
                    return Result.success(new UserStatusSTO("????????????" + index + "???????????????????????????"), "????????????");
                } else {
                    return Result.success(new UserStatusSTO("??????????????????????????????????????????????????????"), "????????????");
                }
            }
            index++;
        }

        for (LockTask lockTask : dynamicInfo.getLockTaskList()) {
            if (lockTask.getAccount().getId().equals(id)) {
                return Result.success(new UserStatusSTO("??????????????????????????????"), "????????????");
            }
        }
        var san = dynamicInfo.getUserSanList().get(id);
        var maxSan = dynamicInfo.getUserMaxSanList().get(id);
        LocalDateTime nextTime = LocalDateTime.now()
                .plusMinutes((maxSan - san) * 6L);
        String nextTimeStr = nextTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        return Result.success(new UserStatusSTO("???????????????????????????" + nextTimeStr + "??????"), "????????????");
    }

    @Override
    public Result<String> showMySan(Long id) {
        var ans = "";
        if (!dynamicInfo.getUserSanList().containsKey(id)) {
            ans = "???????????????";
        } else if (dynamicInfo.getUserSanList().get(id).equals(dynamicInfo.getUserMaxSanList().get(id))) {
            ans = "????????????????????????????????????";
        } else if (dynamicInfo.getLockTaskList().stream().anyMatch(e -> e.getAccount().getId().equals(id))) {
            ans = "?????????????????????????????????";
        } else if (dynamicInfo.getUserSanList().get(id) == null || dynamicInfo.getUserMaxSanList().get(id) == null) {
            ans = "??????????????????????????????";
        } else {
            ans = dynamicInfo.getUserSanList().get(id) + "/" + dynamicInfo.getUserMaxSanList().get(id);
        }

        return Result.success(ans, "????????????");
    }

    @Override
    public Result<String> useCDK(Long id, String cdk) {
        return cdkService.activateCDK(id, cdk);
    }

    @Override
    public Result<String> getWechatQRCode(Long id) {
        Result<String> result = new Result<>();
        if (enableWxPusher) {
            var account = accountMapper.selectOne(
                    Wrappers.<AccountEntity>lambdaQuery()
                            .eq(AccountEntity::getId, id)
            );
            if (account == null) {
                result.setCode(403);
                result.setMsg("???????????????");
                return result;
            }
            CreateQrcodeReq createQrcodeReq = new CreateQrcodeReq();
            createQrcodeReq.setAppToken(appToken);
            createQrcodeReq.setExtra(String.valueOf(id));
            createQrcodeReq.setValidTime(3600);
            var qrcode = WxPusher.createAppTempQrcode(createQrcodeReq);
            if (qrcode.isSuccess()) {
                return result.setCode(200).setMsg("success").setData(qrcode.getData().getUrl());
            } else {
                return result.setCode(403).setMsg("?????????????????????");
            }
        } else {
            return result.setCode(403).setMsg("?????????????????????");
        }
    }

    @Override
    public Result<String> forceHalt(Long id) {
        taskService.forceHaltTask(id);
        return Result.success("????????????");
    }

    @Override
    public Result<String> startNow(Long id) {
        return Result.success(accountService.forceFightAccount(id, false));
    }

    @Override
    public Result<Integer> getRefresh(Long id) {
        var account = accountMapper.selectById(id);
        return Result.success(account.getRefresh(), "????????????");
    }

}
