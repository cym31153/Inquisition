package moe.dazecake.inquisition.service.impl;

import lombok.extern.slf4j.Slf4j;
import moe.dazecake.inquisition.mapper.AccountMapper;
import moe.dazecake.inquisition.model.dto.log.AddLogDTO;
import moe.dazecake.inquisition.model.entity.AccountEntity;
import moe.dazecake.inquisition.model.entity.TaskDateSet.LockTask;
import moe.dazecake.inquisition.service.intf.TaskService;
import moe.dazecake.inquisition.utils.DynamicInfo;
import moe.dazecake.inquisition.utils.TimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    @Resource
    DynamicInfo dynamicInfo;

    @Resource
    LogServiceImpl logService;

    @Resource
    MessageServiceImpl messageService;

    @Resource
    HttpServiceImpl httpService;

    @Resource
    AccountMapper accountMapper;

    @Value("${spring.mail.enable:false}")
    boolean enableMail;

    @Value("${wx-pusher.enable:false}")
    boolean enableWxPusher;


    //检查是否处于时间激活区间，如果是，则返回true，否则返回false
    @Override
    public boolean checkActivationTime(AccountEntity account) {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;

        switch (dayOfWeek) {
            case 1:
                if (account.getActive().getMonday().isEnable()) {

                    if (account.getActive().getMonday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getMonday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getMonday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 2:
                if (account.getActive().getTuesday().isEnable()) {

                    if (account.getActive().getTuesday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getTuesday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getTuesday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 3:
                if (account.getActive().getWednesday().isEnable()) {

                    if (account.getActive().getWednesday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getWednesday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getWednesday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 4:
                if (account.getActive().getThursday().isEnable()) {

                    if (account.getActive().getThursday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getThursday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getThursday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 5:
                if (account.getActive().getFriday().isEnable()) {

                    if (account.getActive().getFriday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getFriday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getFriday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 6:
                if (account.getActive().getSaturday().isEnable()) {

                    if (account.getActive().getSaturday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getSaturday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getSaturday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
            case 0:
                if (account.getActive().getSunday().isEnable()) {

                    if (account.getActive().getSunday().getDetail().isEmpty()) {
                        break;
                    } else {
                        //遍历非激活时间区间
                        for (int i1 = 0; i1 < account.getActive().getSunday().getDetail().size(); i1++) {
                            String[] time = account.getActive().getSunday().getDetail().get(i1).split("-");

                            //处于非激活时间内
                            if (TimeUtil.isInTime(time[0], time[1])) {
                                //不通过
                                return false;
                            }
                        }
                    }

                } else {
                    return false;
                }
                break;
        }
        return true;
    }

    @Override
    public boolean checkFreeze(AccountEntity account) {
        if (dynamicInfo.getFreezeTaskList().containsKey(account.getId())) {
            //检测是否结束冻结
            if (dynamicInfo.getFreezeTaskList().get(account.getId()).isBefore(LocalDateTime.now())) {
                dynamicInfo.getFreezeTaskList().remove(account.getId());
                //解冻，不在冻结状态
                return false;
            }
            //仍处于冻结
            return true;

        } else {
            //不在冻结状态
            return false;
        }
    }

    @Override
    public void lockTask(String deviceToken, AccountEntity account) {
        LocalDateTime localDateTime = LocalDateTime.now();
        var lockTask = new LockTask();
        lockTask.setDeviceToken(deviceToken);
        switch (account.getTaskType()) {
            case "daily":
                lockTask.setAccount(account);
                lockTask.setExpirationTime(localDateTime.plusHours(2));
                break;
            case "rogue":
                lockTask.setAccount(account);
                lockTask.setExpirationTime(localDateTime.plusHours(48));
                break;
            case "rogue2":
                lockTask.setAccount(account);
                lockTask.setExpirationTime(localDateTime.plusHours(72));
                break;
        }
        dynamicInfo.getLockTaskList().add(lockTask);
    }

    @Override
    public void log(String deviceToken, AccountEntity account, String level, String title,
                    String content, String imgUrl) {
        var addLogDTO = new AddLogDTO();
        String type = "";
        if (Objects.equals(account.getTaskType(), "daily")) {
            type = "每日";
        } else if (Objects.equals(account.getTaskType(), "rogue")) {
            type = "肉鸽";
        }

        String detail =
                "[" + type + "] [" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] " + type +
                        content;

        addLogDTO.setLevel(level)
                .setTaskType(account.getTaskType())
                .setTitle(title)
                .setDetail(detail)
                .setImageUrl(imgUrl)
                .setFrom(deviceToken)
                .setServer(account.getServer())
                .setName(account.getName())
                .setAccount(account.getAccount());

        logService.addLog(addLogDTO, false);
    }

    @Override
    public void errorHandle(AccountEntity account, String deviceToken, String type) {

        switch (type) {
            case ("lineBusy"): {
                forceHaltTask(account, false);
                dynamicInfo.getFreezeTaskList().put(account.getId(), LocalDateTime.now().plusHours(1));
                dynamicInfo.getFreeTaskList().add(account);
                break;
            }
            case ("accountError"): {
                if (account.getServer() == 0) {
                    forceHaltTask(account, false);
                    if (httpService.isOfficialAccountWork(account.getAccount(), account.getPassword())) {
                        dynamicInfo.getFreezeTaskList().put(account.getId(), LocalDateTime.now().plusHours(1));
                        dynamicInfo.getFreeTaskList().add(account);
                    } else {
                        account.setFreeze(1);
                        accountMapper.updateById(account);
                        dynamicInfo.getUserSanList().remove(account.getId());
                        dynamicInfo.getUserMaxSanList().remove(account.getId());
                        messageService.push(account, "账号异常", "您的账号密码有误，请在面板更新正确的账号密码，否则托管将无法继续进行");
                    }
                } else if (account.getServer() == 1) {
                    forceHaltTask(account, false);
                    if (httpService.isBiliAccountWork(account.getAccount(), account.getPassword())) {
                        messageService.push(account, "账号异常", "您近期登陆的设备较多，已被B服限制登陆，请立即修改密码并于面板更新密码,否则托管可能将无法继续进行");
                    } else {
                        account.setFreeze(1);
                        accountMapper.updateById(account);
                        dynamicInfo.getUserSanList().remove(account.getId());
                        dynamicInfo.getUserMaxSanList().remove(account.getId());
                        messageService.push(account, "账号异常", "您的账号密码有误，请在面板更新正确的账号密码，否则托管将无法继续进行");
                    }
                }
            }
            default: {
                forceHaltTask(account, false);
                dynamicInfo.getFreezeTaskList().put(account.getId(), LocalDateTime.now().plusHours(1));
                dynamicInfo.getFreeTaskList().add(account);
                break;
            }
        }
    }

    @Override
    public void forceHaltTask(Long id) {
        for (AccountEntity freeTask : dynamicInfo.getFreeTaskList()) {
            if (freeTask.getId().equals(id)) {
                dynamicInfo.getFreeTaskList().remove(freeTask);
                break;
            }
        }
        for (LockTask lockTask : dynamicInfo.getLockTaskList()) {
            if (lockTask.getAccount().getId().equals(id)) {
                dynamicInfo.getLockTaskList().remove(lockTask);
                dynamicInfo.getHaltList().add(lockTask.getDeviceToken());
                break;
            }
        }
        dynamicInfo.getFreezeTaskList().remove(id);
    }

    // TODO: 2023/1/26 删除此用法
    @Override
    public void forceHaltTask(AccountEntity account, boolean notHalted) {
        //清除等待队列
        for (AccountEntity entity : dynamicInfo.getFreeTaskList()) {
            if (entity.getId().equals(account.getId())) {
                dynamicInfo.getFreeTaskList().remove(entity);
                break;
            }
        }

        //清除上锁队列
        for (LockTask lockTask : dynamicInfo.getLockTaskList()) {
            if (lockTask.getAccount().getId().equals(account.getId())) {
                if (notHalted) {
                    dynamicInfo.getHaltList().add(lockTask.getDeviceToken());
                }
                dynamicInfo.getLockTaskList().remove(lockTask);
                break;
            }
        }

        //清除冻结队列
        dynamicInfo.getFreezeTaskList().remove(account.getId());
    }

    @Override
    public void calculatingSan() {
        //检查两个表是否存在不同步的情况
        dynamicInfo.getUserSanList().forEach((k, v) -> {
            if (!dynamicInfo.getUserMaxSanList().containsKey(k)) {
                dynamicInfo.getUserMaxSanList().put(k, 135);
            }
        });

        //获取迭代器
        Iterator<Map.Entry<Long, Integer>> entryIterator = dynamicInfo.getUserSanList().entrySet().iterator();

        //遍历所有用户
        while (entryIterator.hasNext()) {
            Long id = entryIterator.next().getKey();

            var account = accountMapper.selectById(id);

            //检查是否已删除
            if (account.getDelete() == 1) {
                entryIterator.remove();
                dynamicInfo.getUserMaxSanList().remove(id);
                continue;
            }

            //检查是否已冻结
            if (account.getFreeze() == 1) {
                entryIterator.remove();
                dynamicInfo.getUserMaxSanList().remove(id);
                continue;
            }

            //检查是否已到期
            if (account.getExpireTime().isBefore(LocalDateTime.now())) {
                entryIterator.remove();
                dynamicInfo.getUserMaxSanList().remove(id);
                messageService.push(account, "到期提醒", "您的账号已到期，作战已暂停，若仍需托管请及时续费");
                continue;
            }

            //递增用户理智
            dynamicInfo.getUserSanList().put(id, dynamicInfo.getUserSanList().get(id) + 1);

            //检查是否到达阈值 阈值为最大值-40
            if (dynamicInfo.getUserSanList().get(id) >= dynamicInfo.getUserMaxSanList().get(id) - 40) {

                //检查待分配队列中是否有重复任务
                dynamicInfo.getFreeTaskList().removeIf(accountEntity -> accountEntity.getId().equals(account.getId()));

                //加入待分配队列
                dynamicInfo.getFreeTaskList().add(account);

                messageService.push(account, "等待分配作战服务器", "您的理智已达到 " + dynamicInfo.getUserSanList().get(id) +
                        "，等待分配作战服务器中，分配完成后将会自动开始作战");

                //归零理智
                dynamicInfo.getUserSanList().put(id, 0);
            }

            //检查是否到达提醒阈值 阈值为最大值-45
            if (dynamicInfo.getUserSanList().get(id) == dynamicInfo.getUserMaxSanList().get(id) - 45) {
                messageService.push(account, "作战预告", "您的账号最快将在30" +
                        "分钟后开始作战，若您当前仍在线，请注意合理把握时间，避免被强制下线\n\n" +
                        "若您需要轮空本次作战，请前往面板-->设置-->冻结，手动冻结账号来进行轮空\n\n" +
                        "当前理智: " +
                        dynamicInfo.getUserSanList().get(id) +
                        "/" +
                        dynamicInfo.getUserMaxSanList().get(id) + "\n\n" +
                        "(可能存在误差，仅供参考)");
            }

        }

    }
}
