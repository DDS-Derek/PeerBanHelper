package com.ghostchu.peerbanhelper.module.impl.rule;

import com.ghostchu.peerbanhelper.PeerBanHelperServer;
import com.ghostchu.peerbanhelper.module.AbstractRuleFeatureModule;
import com.ghostchu.peerbanhelper.module.BanResult;
import com.ghostchu.peerbanhelper.module.PeerAction;
import com.ghostchu.peerbanhelper.peer.Peer;
import com.ghostchu.peerbanhelper.text.Lang;
import com.ghostchu.peerbanhelper.torrent.Torrent;
import com.ghostchu.peerbanhelper.util.IPAddressUtil;
import com.ghostchu.peerbanhelper.wrapper.PeerAddress;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Getter;
import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class IPBlackList extends AbstractRuleFeatureModule {
    private List<IPAddress> ips;
    private List<Integer> ports;
    private Map<Object, Map<String, AtomicLong>> counter;

    public IPBlackList(PeerBanHelperServer server, YamlConfiguration profile) {
        super(server, profile);
    }

    @Override
    public @NotNull String getName() {
        return "IP Blacklist";
    }

    @Override
    public @NotNull String getConfigName() {
        return "ip-address-blocker";
    }

    @Override
    public boolean isCheckCacheable() {
        return true;
    }

    @Override
    public boolean needCheckHandshake() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void onEnable() {
        reloadConfig();
    }

    @Override
    public void onDisable() {

    }

    private void reloadConfig() {
        this.ips = new ArrayList<>();
        for (String s : getConfig().getStringList("ips")) {
            IPAddress ipAddress = new IPAddressString(s).getAddress();
            if (ipAddress != null) {
                if (ipAddress.isIPv4Convertible()) {
                    ipAddress = ipAddress.toIPv4();
                }
                this.ips.add(ipAddress);
            }
        }
        this.ports = getConfig().getIntList("ports");
        this.counter = new LinkedHashMap<>(ips.size() + ports.size());
    }

    @Override
    public @NotNull BanResult shouldBanPeer(@NotNull Torrent torrent, @NotNull Peer peer, @NotNull ExecutorService ruleExecuteExecutor) {
        PeerAddress peerAddress = peer.getAddress();
        if (ports.contains(peerAddress.getPort())) {
            return new BanResult(this, PeerAction.BAN, String.valueOf(peerAddress.getPort()), String.format(Lang.MODULE_IBL_MATCH_PORT, peerAddress.getPort()));
        }
        IPAddress pa = IPAddressUtil.getIPAddress(peerAddress.getIp());
        if (pa.isIPv4Convertible()) {
            pa = pa.toIPv4();
        }
        for (IPAddress ra : ips) {
            if (ra.isIPv4() != pa.isIPv4()) { // 在上面的规则处统一进行过转换，此处可直接进行检查
                continue;
            }
            if (ra.equals(pa) || ra.contains(pa)) {
                return new BanResult(this, PeerAction.BAN, ra.toString(), String.format(Lang.MODULE_IBL_MATCH_IP, ra));
            }
        }
        return new BanResult(this, PeerAction.NO_ACTION, "N/A", "No matches");
    }

}
