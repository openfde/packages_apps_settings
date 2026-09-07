/*
**
** Copyright (C) 2021 The WayDroid Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.openfde;
interface INet {
    int setStaticIp(String interfaceName, String ipAddress, int networkPrefixLength, String gateway, String dns1, String dns2);
    int setDHCP(String interfaceName);
    String getAllSsid();
    int connectSsid(String ssid, String passwd);
    String getActivedWifi();
    int connectActivedWifi(String ssid, int connect);
    int enableWifi(int enable);
    String connectedWifiList();
    int isWifiEnable();
    String getSignalAndSecurity(String ssid);
    int connectHidedWifi(String ssid, String passwd);
    int forgetWifi(String ssid);
    String getStaticIpConf(String interfaceName);
    String getActivedInterface();
    String getIpConfigure(String interfaceName);
    String getDns(String interfaceName);
    String getLans();
    String getLansAndWlans();
    String getLanAndWlanIpConfigurations();
    int ipConfiged(String interfaceName);
    String getLansWlansBridges();
    String getLanWlanBridgeIpConfigurations();
    String getAllSsidInfo();
}
