package moe.dazecake.inquisition.utils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import moe.dazecake.inquisition.mapper.AccountMapper;
import moe.dazecake.inquisition.mapper.DeviceMapper;
import moe.dazecake.inquisition.model.entity.AccountEntity;
import moe.dazecake.inquisition.model.entity.DeviceEntity;
import moe.dazecake.inquisition.model.entity.TaskDateSet.LockTask;
import moe.dazecake.inquisition.service.impl.ChinacServiceImpl;
import moe.dazecake.inquisition.service.impl.LogServiceImpl;
import moe.dazecake.inquisition.service.impl.MessageServiceImpl;
import moe.dazecake.inquisition.service.impl.TaskServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;

@Slf4j
@Configuration
@EnableScheduling
public class DynamicScheduleTask implements SchedulingConfigurer {

    @Resource
    DynamicInfo dynamicInfo;

    @Resource
    AccountMapper accountMapper;

    @Resource
    DeviceMapper deviceMapper;

    @Resource
    LogServiceImpl logService;

    @Resource
    MessageServiceImpl messageService;

    @Resource
    TaskServiceImpl taskService;

    @Resource
    ChinacServiceImpl chinacService;

    @Value("${spring.mail.to:}")
    String to;

    @Value("${spring.mail.enable:false}")
    boolean enableMail;

    @Value("${wx-pusher.enable:false}")
    boolean enableWxPusher;

    @Value("${inquisition.chinac.enableAutoDeviceManage:false}")
    boolean enableAutoDeviceManage;

    @Value("${inquisition.chinac.maxPlayerInDevice:25}")
    Integer maxPlayerInDevice;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        //????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    //log.info("??????????????????: " + LocalDateTime.now().toLocalTime());
                    //??????????????????????????????????????????????????????????????????????????????
                    LinkedHashSet<AccountEntity> set = new LinkedHashSet<>(dynamicInfo.freeTaskList);
                    dynamicInfo.freeTaskList = new ArrayList<>(set);
                },
                triggerContext -> new CronTrigger("0 */1 * * * *").nextExecutionTime(triggerContext)
        );
        //????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    //log.info("????????????????????????: " + LocalDateTime.now().toLocalTime());
                    taskService.calculatingSan();
                },
                triggerContext -> new CronTrigger("0 */6 * * * *").nextExecutionTime(triggerContext)
        );
        //??????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    for (java.util.Map.Entry<String, Integer> count : dynamicInfo.getCounter().entrySet()) {

                        var token = count.getKey();
                        var num = count.getValue();

                        --num;
                        dynamicInfo.getCounter().put(token, num);

                        if (num == 0) {
                            dynamicInfo.getDeviceStatusMap().put(token, 0);
                            log.warn("????????????: " + token);
                        } else if (num == -60) {
                            //??????????????????
                            var device = deviceMapper.selectOne(
                                    Wrappers.<DeviceEntity>lambdaQuery()
                                            .eq(DeviceEntity::getDeviceToken, token)
                            );

                            //????????????
                            logService.logWarn("????????????", "????????????: " + device.getDeviceName() + "\n" +
                                    "??????token: " + device.getDeviceToken() + "\n");

                            //????????????
                            messageService.pushAdmin("[?????????] ????????????", "????????????: " + device.getDeviceName() + "\n"
                                    + "??????token: " + device.getDeviceToken() + "\n"
                                    + "??????: " + LocalDateTime.now() + "\n");

                        } else if (num == 86400) {
                            //??????24h???????????????
                            dynamicInfo.getDeviceStatusMap().remove(token);
                            dynamicInfo.getCounter().remove(token);

                            var device = deviceMapper.selectOne(
                                    Wrappers.<DeviceEntity>lambdaQuery()
                                            .eq(DeviceEntity::getDeviceToken, token)
                            );
                            device.setDelete(1);
                            deviceMapper.updateById(device);

                            //????????????
                            logService.logWarn("????????????", "????????????: " + device.getDeviceName() + "\n" +
                                    "??????token: " + device.getDeviceToken() + "\n");

                            //????????????
                            messageService.pushAdmin("[?????????] ????????????", "????????????: " + device.getDeviceName() + "\n"
                                    + "??????token: " + device.getDeviceToken() + "\n"
                                    + "??????: " + LocalDateTime.now() + "\n");
                        }
                    }
                },
                triggerContext -> new CronTrigger("0/5 * * * * ?").nextExecutionTime(triggerContext)
        );
        //??????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    //log.info("??????????????????");
                    LocalDateTime nowTime = LocalDateTime.now();
                    int num = 0;
                    for (LockTask lockTask : dynamicInfo.getLockTaskList()) {
                        if (lockTask.getExpirationTime().isBefore(nowTime)) {
                            //????????????
                            logService.logWarn("????????????", "");
                            taskService.forceHaltTask(lockTask.getAccount().getId());
                            num++;
                        }
                    }
                    if (num > 0) {
                        log.info("????????????????????????: " + num);
                    }
                },
                triggerContext -> new CronTrigger("0 0/5 * * * ?").nextExecutionTime(triggerContext)
        );

        //??????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("??????????????????");
                    var finalTime = LocalDateTime.now().plusDays(7);
                    var accountList = accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                            .lt(AccountEntity::getExpireTime, finalTime)
                            .gt(AccountEntity::getExpireTime, LocalDateTime.now())
                            .eq(AccountEntity::getDelete, 0));
                    accountList.forEach(
                            (account) -> {
                                log.info("??????????????????: " + account.getAccount() + " " + account.getAccount());
                                var msg = "????????????????????????" + account.getExpireTime()
                                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "?????????????????????????????????";

                                messageService.push(account, "????????????????????????????????????", msg);
                            }
                    );
                },
                triggerContext -> new CronTrigger("0 0 20 * * ?").nextExecutionTime(triggerContext)
        );
        //??????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("??????????????????");
                    var accountList = accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                            .gt(AccountEntity::getExpireTime, LocalDateTime.now())
                            .eq(AccountEntity::getFreeze, 1)
                            .eq(AccountEntity::getDelete, 0));
                    accountList.forEach(
                            (account) -> {
                                log.info("??????????????????: " + account.getAccount() + " " + account.getAccount());
                                var msg = "??????????????????????????????????????????????????????????????????????????????????????????????????????????????????";

                                messageService.push(account, "????????????????????????????????????", msg);

                            }
                    );
                },
                triggerContext -> new CronTrigger("0 0 20 * * ?").nextExecutionTime(triggerContext)
        );
        //????????????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    log.info("????????????????????????");
                    var accountList = accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                            .eq(AccountEntity::getRefresh, 0)
                            .eq(AccountEntity::getDelete, 0)
                            .ge(AccountEntity::getExpireTime, LocalDateTime.now())
                    );
                    accountList.forEach(
                            (account) -> {
                                account.setRefresh(1);
                                accountMapper.updateById(account);
                            }
                    );
                },
                triggerContext -> new CronTrigger("0 0 0 * * ?").nextExecutionTime(triggerContext)
        );
        //??????????????????
        taskRegistrar.addTriggerTask(
                () -> {
                    if (!enableAutoDeviceManage) {
                        return;
                    }
                    log.info("??????????????????");
                    var payedUserList = accountMapper.selectList(Wrappers.<AccountEntity>lambdaQuery()
                            .ge(AccountEntity::getExpireTime, LocalDateTime.now())
                            .eq(AccountEntity::getDelete, 0));
                    var deviceList = deviceMapper.selectList(Wrappers.<DeviceEntity>lambdaQuery()
                            .eq(DeviceEntity::getDelete, 0));
                    if (deviceList.size() < payedUserList.size() / maxPlayerInDevice) {
                        var newDevice = chinacService.createDevice(
                                "cn-jsha-cloudphone-3",
                                "805321",
                                "PREPAID",
                                0,
                                1,
                                null, null, null);
                        if (newDevice == null) {
                            messageService.pushAdmin("[?????????] ????????????????????????", "??????????????????????????????????????????????????????");
                            return;
                        }
                        SimpleDateFormat format = new SimpleDateFormat("MM_dd");
                        String time = format.format(new Date().getTime());
                        deviceMapper.insert(new DeviceEntity()
                                .setDeviceName("?????????_" + time)
                                .setRegion("")
                                .setDeviceToken(newDevice.get(0))
                                .setDelete(0)
                        );
                        String text = "??????????????????: " + payedUserList.size() + "\n" +
                                "????????????: " + deviceList.size() + "\n" +
                                "??????????????????????????????????????????????????????";
                        messageService.pushAdmin("[?????????] ??????????????????", text);
                    }
                    log.info("??????????????????");

                    //?????????????????????????????? ??????????????????????????????: 2
                    var overNum = (payedUserList.size() - deviceList.size() * maxPlayerInDevice) / maxPlayerInDevice;
                    //????????????????????????
                    deviceList.removeIf(device -> device.getChinac() != 1);
                    if (overNum > 2) {
                        for (int i = 0; i < overNum; i++) {
                            Iterator<DeviceEntity> iterator = deviceList.iterator();
                            var flagDevice = iterator.next();
                            while (iterator.hasNext()) {
                                var device = iterator.next();
                                if (flagDevice.getExpireTime().isBefore(device.getExpireTime())) {
                                    flagDevice = device;
                                }
                            }
                            deviceList.remove(flagDevice);
                        }
                    }
                    for (DeviceEntity device : deviceList) {
                        if (device.getExpireTime().isBefore(LocalDateTime.now().plusDays(7)) && device.getChinac() == 1) {
                            if (chinacService.renewDevice(device.getRegion(), device.getDeviceToken(), 1)) {
                                String text = "????????????: " + device.getDeviceName() + "\n" +
                                        "?????????????????????????????????????????????";
                                messageService.pushAdmin("[?????????] ??????????????????", text);
                            } else {
                                String text = "??????????????????????????????????????????????????????";
                                messageService.pushAdmin("[?????????] ????????????????????????", text);
                            }
                            break;
                        }
                    }
                },
                triggerContext -> new CronTrigger("0 0 20 * * ?").nextExecutionTime(triggerContext)
        );
    }
}
