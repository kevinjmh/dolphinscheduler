/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dolphinscheduler.alert.plugin;

import org.apache.dolphinscheduler.alert.utils.Constants;
import org.apache.dolphinscheduler.alert.utils.PropertyUtils;
import org.apache.dolphinscheduler.common.utils.CollectionUtils;
import org.apache.dolphinscheduler.common.utils.StringUtils;
import org.apache.dolphinscheduler.plugin.api.AlertPlugin;
import org.apache.dolphinscheduler.plugin.model.AlertData;
import org.apache.dolphinscheduler.plugin.model.AlertInfo;
import org.apache.dolphinscheduler.plugin.model.PluginName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

public class CurlAlertPlugin implements AlertPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CurlAlertPlugin.class);

    private PluginName pluginName;

    public CurlAlertPlugin() {
        this.pluginName = new PluginName();
        this.pluginName.setEnglish("curl");
        this.pluginName.setChinese("curl");
    }

    @Override
    public String getId() {
        return "curl";
    }

    @Override
    public PluginName getName() {
        return pluginName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(AlertInfo info) {
        Map<String, Object> retMaps = new HashMap<>();

        AlertData alert = info.getAlertData();

        // contains users' email in alter group
        // ignore alter group type
        List<String> receviersList = (List<String>) info.getProp("receivers");

        // get receivers
        String receivers = alert.getReceivers();
        if (StringUtils.isNotEmpty(receivers)) {
            String[] splits = receivers.split(",");
            receviersList.addAll(Arrays.asList(splits));
        }

        String receiversCc = alert.getReceiversCc();
        if (StringUtils.isNotEmpty(receiversCc)) {
            String[] splits = receiversCc.split(",");
            receviersList.addAll(Arrays.asList(splits));
        }

        // get phone from email
        HashSet<String> phoneSet = new HashSet<>();
        for (String email: receviersList) {
            String phone = email.split("@")[0];
            if (phone.length() == 11) {
                phoneSet.add(phone);
            } else {
                logger.error("alert email does not contain correct phone number:" + email);
            }
        }

        if (phoneSet.isEmpty()) {
            retMaps.put(Constants.STATUS, "false");
            retMaps.put(Constants.MESSAGE, "At least one receiver address required.");
            logger.warn(alert.getContent());
            return retMaps;
        }
        String phones = String.join("/", phoneSet);

        // Avoid long message sending failure.
        String msgContent = alert.getContent().replace("\"","");
        if (msgContent.length() > 250) {
            msgContent = msgContent.substring(0, 250) + "...";
        }
        String msg = String.format("【%s】%s", alert.getTitle(), msgContent);
        logger.info("Message will be sent to {}: {}", phones, msg);

        try {
            msg = URLEncoder.encode(msg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            retMaps.put(Constants.STATUS, "false");
            retMaps.put(Constants.MESSAGE, "encode message failed.");
            return retMaps;
        }

        try {
            String responseMsg = execCurl(PropertyUtils.getString("curl_addr"), msg, phones);
            retMaps.put(Constants.STATUS, "true");
            retMaps.put(Constants.MESSAGE, responseMsg);
        } catch (IOException e) {
            retMaps.put(Constants.STATUS, "false");
            retMaps.put(Constants.MESSAGE, e.getMessage());
            e.printStackTrace();
            return null;
        }
        return retMaps;
    }

    public String execCurl(String address, String msg, String phones) throws IOException {
        String[] cmds;
        if (null == address) {
            // for TEST
            cmds = new String[]{"echo", "Test"};
            logger.info("Testing: " + msg);
            logger.info("phones: " + phones);
        } else {
            if (!address.contains("message") || !address.contains("12345678xxx")) {
                logger.info("curl_addr format is wrong. Example: " +
                        "http://ip:port/sms/getReport?content=message&mobilesStr=12345678xxx");
                return "";
            }
            cmds = new String[]{"curl", "-s",
                    address.replace("message", msg).replace("12345678xxx",phones) };
        }

        //HTTP请求有两个超时时间：一个是连接超时时间，另一个是数据传输的最大允许时间（请求资源超时时间）。 单位秒
        //String[] cmds = {"curl","--connect-timeout","5","m","6", "-H", "Content-Type:application/json", "-X","POST","--data",""+msg+"",""+address+""};

        ProcessBuilder process = new ProcessBuilder(cmds);
        Process p;
        p = process.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

}
