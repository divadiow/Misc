package uk.co.divadiow.hotspotdevices;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(11, 15, 20);
    private static final int CARD = Color.rgb(21, 27, 35);
    private static final int BORDER = Color.rgb(38, 50, 65);
    private static final int ACCENT = Color.rgb(79, 163, 255);
    private static final int PRIMARY = Color.rgb(242, 246, 250);
    private static final int SECONDARY = Color.rgb(159, 176, 194);
    private static final int SUCCESS = Color.rgb(98, 211, 148);
    private static final int WARNING = Color.rgb(255, 206, 115);
    private static final int DANGER = Color.rgb(255, 122, 122);

    private static final int[] DISCOVERY_PORTS = {80, 443, 22};
    private static final int[] SERVICE_PORTS = {22, 23, 53, 80, 443, 554, 1883, 5000, 8000, 8080, 8883, 8888, 9100};

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelScan = new AtomicBoolean(false);
    private final Map<String, DeviceRecord> records = new LinkedHashMap<>();

    private EditText subnetInput;
    private TextView interfaceText;
    private TextView statusText;
    private TextView countText;
    private ProgressBar progressBar;
    private LinearLayout devicesContainer;
    private Button scanButton;
    private Button stopButton;
    private Button shareButton;
    private CheckBox autoRefresh;
    private ExecutorService scanExecutor;
    private boolean scanRunning;
    private String ownAddress;
    private String selectedInterface;
    private String selectedCidr;

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (autoRefresh.isChecked() && !scanRunning) startScan();
            if (autoRefresh.isChecked()) mainHandler.postDelayed(this, 10_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
        detectNetwork();
    }

    @Override
    protected void onDestroy() {
        cancelCurrentScan();
        mainHandler.removeCallbacks(autoRefreshRunnable);
        super.onDestroy();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(text("Hotspot Devices", 28, PRIMARY, true));
        TextView subtitle = text("Find IP addresses on your phone's hotspot without root or Termux.", 15, SECONDARY, false);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        LinearLayout networkCard = card();
        root.addView(networkCard, margins(-1, -2, 0, 0, 0, 12));
        networkCard.addView(label("DETECTED NETWORK"));
        interfaceText = text("Detecting hotspot interface…", 15, PRIMARY, false);
        interfaceText.setPadding(0, dp(7), 0, dp(10));
        networkCard.addView(interfaceText);

        subnetInput = new EditText(this);
        subnetInput.setTextColor(PRIMARY);
        subnetInput.setHintTextColor(SECONDARY);
        subnetInput.setHint("10.20.167.0/24");
        subnetInput.setSingleLine(true);
        subnetInput.setTextSize(17);
        subnetInput.setTypeface(Typeface.MONOSPACE);
        subnetInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        subnetInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        subnetInput.setBackground(roundRect(Color.rgb(10, 14, 19), BORDER, 10));
        networkCard.addView(subnetInput, new LinearLayout.LayoutParams(-1, -2));

        Button redetect = button("Detect again", false);
        redetect.setOnClickListener(v -> detectNetwork());
        networkCard.addView(redetect, margins(-1, dp(44), 0, 10, 0, 0));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(actions, margins(-1, -2, 0, 0, 0, 12));

        scanButton = button("Scan now", true);
        scanButton.setOnClickListener(v -> startScan());
        actions.addView(scanButton, new LinearLayout.LayoutParams(0, dp(50), 1f));

        stopButton = button("Stop", false);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> cancelCurrentScan());
        actions.addView(stopButton, margins(dp(88), dp(50), 10, 0, 0, 0));

        shareButton = button("Share", false);
        shareButton.setEnabled(false);
        shareButton.setOnClickListener(v -> shareReport());
        actions.addView(shareButton, margins(dp(88), dp(50), 10, 0, 0, 0));

        LinearLayout optionsCard = card();
        root.addView(optionsCard, margins(-1, -2, 0, 0, 0, 12));
        autoRefresh = new CheckBox(this);
        autoRefresh.setText("Auto-refresh every 10 seconds while the app is open");
        autoRefresh.setTextColor(PRIMARY);
        autoRefresh.setTextSize(15);
        autoRefresh.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        autoRefresh.setOnCheckedChangeListener((buttonView, checked) -> {
            mainHandler.removeCallbacks(autoRefreshRunnable);
            if (checked) mainHandler.post(autoRefreshRunnable);
        });
        optionsCard.addView(autoRefresh);

        statusText = text("Ready", 15, SECONDARY, false);
        statusText.setPadding(0, 0, 0, dp(8));
        root.addView(statusText);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(8)));

        countText = text("No scan results yet", 20, PRIMARY, true);
        countText.setPadding(0, dp(18), 0, dp(10));
        root.addView(countText);

        devicesContainer = new LinearLayout(this);
        devicesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(devicesContainer, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout noteCard = card();
        root.addView(noteCard, margins(-1, -2, 0, 14, 0, 0));
        noteCard.addView(label("LIMITATIONS"));
        TextView note = text("Android blocks ordinary apps from reading the hotspot DHCP and ARP tables. This app therefore discovers clients by making short local-network probes. It can report IP addresses and responsive TCP services, but usually cannot obtain MAC addresses or device names.", 14, SECONDARY, false);
        note.setPadding(0, dp(7), 0, 0);
        noteCard.addView(note);

        return scroll;
    }

    private void detectNetwork() {
        status("Detecting hotspot interface…", SECONDARY);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<InterfaceCandidate> candidates = NetworkDetector.findCandidates();
            mainHandler.post(() -> {
                if (candidates.isEmpty()) {
                    selectedInterface = null;
                    selectedCidr = null;
                    ownAddress = null;
                    interfaceText.setText("No suitable private IPv4 interface detected. Enter the hotspot subnet manually.");
                    status("Enter a subnet such as 10.20.167.0/24.", WARNING);
                    return;
                }
                InterfaceCandidate best = candidates.get(0);
                selectedInterface = best.name;
                selectedCidr = best.networkCidr;
                ownAddress = best.address;
                interfaceText.setText(best.name + "  •  phone " + best.address + "/" + best.prefixLength);
                subnetInput.setText(best.networkCidr);
                status("Ready to scan " + best.networkCidr, SUCCESS);
            });
        });
    }

    private void startScan() {
        if (scanRunning) return;
        final Subnet subnet;
        try {
            subnet = Subnet.parse(subnetInput.getText().toString().trim());
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (subnet.hostCount() > 1024) {
            Toast.makeText(this, "That network is too large. Use /22 or a smaller range such as /24.", Toast.LENGTH_LONG).show();
            return;
        }

        selectedCidr = subnet.cidr();
        for (DeviceRecord record : records.values()) record.seenThisScan = false;
        renderDevices();
        cancelScan.set(false);
        scanRunning = true;
        scanButton.setEnabled(false);
        stopButton.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        status("Scanning " + selectedCidr + "…", SECONDARY);

        scanExecutor = Executors.newFixedThreadPool(48);
        ExecutorService coordinator = Executors.newSingleThreadExecutor();
        coordinator.execute(() -> runScan(subnet));
        coordinator.shutdown();
    }

    private void runScan(Subnet subnet) {
        List<String> hosts = subnet.hostAddresses();
        if (ownAddress != null) hosts.remove(ownAddress);
        int total = hosts.size();
        CompletionService<DeviceResult> completion = new ExecutorCompletionService<>(scanExecutor);
        List<Future<DeviceResult>> futures = new ArrayList<>();
        for (String ip : hosts) {
            if (cancelScan.get()) break;
            futures.add(completion.submit(() -> probeHost(ip)));
        }

        int completed = 0;
        int found = 0;
        boolean permissionProblem = false;
        while (completed < futures.size() && !cancelScan.get()) {
            try {
                Future<DeviceResult> future = completion.poll(500, TimeUnit.MILLISECONDS);
                if (future == null) continue;
                DeviceResult result = future.get();
                completed++;
                if (result.permissionDenied) permissionProblem = true;
                if (result.alive) {
                    found++;
                    mainHandler.post(() -> addOrUpdateRecord(result));
                }
                final int progress = total == 0 ? 100 : Math.min(100, (completed * 100) / total);
                final int uiFound = found;
                final int uiCompleted = completed;
                mainHandler.post(() -> {
                    progressBar.setProgress(progress);
                    statusText.setText("Scanned " + uiCompleted + " of " + total + " addresses  •  " + uiFound + " device" + (uiFound == 1 ? "" : "s") + " found");
                });
            } catch (Exception ignored) {
                completed++;
            }
        }

        for (Future<DeviceResult> future : futures) if (!future.isDone()) future.cancel(true);
        scanExecutor.shutdownNow();
        final boolean cancelled = cancelScan.get();
        final boolean blocked = permissionProblem;
        mainHandler.post(() -> finishScan(cancelled, blocked));
    }

    private DeviceResult probeHost(String ip) {
        DeviceResult result = new DeviceResult(ip);
        long started = System.nanoTime();
        try {
            InetAddress address = InetAddress.getByName(ip);
            try {
                if (address.isReachable(170)) result.alive = true;
            } catch (SecurityException ex) {
                result.permissionDenied = true;
            }

            if (!result.alive) {
                for (int port : DISCOVERY_PORTS) {
                    ProbeOutcome outcome = connectProbe(ip, port, 240);
                    if (outcome == ProbeOutcome.PERMISSION_DENIED) result.permissionDenied = true;
                    if (outcome == ProbeOutcome.OPEN || outcome == ProbeOutcome.REFUSED) {
                        result.alive = true;
                        if (outcome == ProbeOutcome.OPEN) result.openPorts.add(port);
                        break;
                    }
                }
            }

            if (result.alive) {
                for (int port : SERVICE_PORTS) {
                    if (cancelScan.get() || Thread.currentThread().isInterrupted()) break;
                    if (result.openPorts.contains(port)) continue;
                    ProbeOutcome outcome = connectProbe(ip, port, 130);
                    if (outcome == ProbeOutcome.OPEN) result.openPorts.add(port);
                    if (outcome == ProbeOutcome.PERMISSION_DENIED) result.permissionDenied = true;
                }
            }
        } catch (Exception ignored) {
        }
        result.latencyMs = Math.max(1, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        return result;
    }

    private ProbeOutcome connectProbe(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            return ProbeOutcome.OPEN;
        } catch (ConnectException ex) {
            String message = String.valueOf(ex.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("refused") || message.contains("econnrefused")) return ProbeOutcome.REFUSED;
            if (message.contains("permission") || message.contains("eperm")) return ProbeOutcome.PERMISSION_DENIED;
            return ProbeOutcome.NO_RESPONSE;
        } catch (SecurityException ex) {
            return ProbeOutcome.PERMISSION_DENIED;
        } catch (SocketTimeoutException ex) {
            return ProbeOutcome.NO_RESPONSE;
        } catch (IOException ex) {
            String message = String.valueOf(ex.getMessage()).toLowerCase(Locale.ROOT);
            if (message.contains("refused") || message.contains("econnrefused")) return ProbeOutcome.REFUSED;
            if (message.contains("permission") || message.contains("eperm")) return ProbeOutcome.PERMISSION_DENIED;
            return ProbeOutcome.NO_RESPONSE;
        }
    }

    private void addOrUpdateRecord(DeviceResult result) {
        DeviceRecord record = records.get(result.ip);
        long now = System.currentTimeMillis();
        if (record == null) {
            record = new DeviceRecord(result.ip, now);
            records.put(result.ip, record);
        }
        record.lastSeen = now;
        record.lastLatencyMs = result.latencyMs;
        record.openPorts.clear();
        record.openPorts.addAll(result.openPorts);
        record.seenThisScan = true;
        renderDevices();
    }

    private void renderDevices() {
        devicesContainer.removeAllViews();
        List<DeviceRecord> sorted = new ArrayList<>(records.values());
        sorted.sort(Comparator.comparingLong(record -> ipv4ToLong(record.ip)));
        int current = 0;
        for (DeviceRecord record : sorted) {
            if (record.seenThisScan) current++;
            devicesContainer.addView(deviceCard(record), margins(-1, -2, 0, 0, 0, 10));
        }
        countText.setText(current + " connected device" + (current == 1 ? "" : "s") + (records.size() > current ? "  •  " + records.size() + " seen this session" : ""));
        shareButton.setEnabled(!records.isEmpty());
    }

    private View deviceCard(DeviceRecord record) {
        LinearLayout card = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, new LinearLayout.LayoutParams(-1, -2));

        TextView ipText = text(record.ip, 21, PRIMARY, true);
        ipText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        top.addView(ipText, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView state = text(record.seenThisScan ? "ONLINE" : "PREVIOUS", 12, record.seenThisScan ? SUCCESS : SECONDARY, true);
        state.setGravity(Gravity.CENTER);
        state.setPadding(dp(9), dp(5), dp(9), dp(5));
        state.setBackground(roundRect(Color.TRANSPARENT, record.seenThisScan ? SUCCESS : BORDER, 20));
        top.addView(state);

        String services = record.openPorts.isEmpty() ? "No common TCP service found" : formatPorts(record.openPorts);
        TextView serviceText = text(services, 14, record.openPorts.isEmpty() ? SECONDARY : WARNING, false);
        serviceText.setPadding(0, dp(9), 0, dp(5));
        card.addView(serviceText);

        String timing = "First seen " + time(record.firstSeen) + "  •  Last seen " + time(record.lastSeen) + "  •  probe " + record.lastLatencyMs + " ms";
        card.addView(text(timing, 12, SECONDARY, false));

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(10), 0, 0);
        scroller.addView(buttons);
        card.addView(scroller, new LinearLayout.LayoutParams(-1, -2));

        Button copy = smallButton("Copy IP");
        copy.setOnClickListener(v -> copyIp(record.ip));
        buttons.addView(copy);

        if (record.openPorts.contains(80) || record.openPorts.contains(8000) || record.openPorts.contains(8080) || record.openPorts.contains(8888)) {
            int port = firstPresent(record.openPorts, 80, 8000, 8080, 8888);
            Button open = smallButton("Open HTTP");
            open.setOnClickListener(v -> openUrl("http://" + record.ip + (port == 80 ? "" : ":" + port)));
            buttons.addView(open, margins(-2, dp(38), 8, 0, 0, 0));
        }
        if (record.openPorts.contains(443)) {
            Button open = smallButton("Open HTTPS");
            open.setOnClickListener(v -> openUrl("https://" + record.ip));
            buttons.addView(open, margins(-2, dp(38), 8, 0, 0, 0));
        }
        return card;
    }

    private void finishScan(boolean cancelled, boolean permissionProblem) {
        scanRunning = false;
        scanButton.setEnabled(true);
        stopButton.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        renderDevices();

        if (cancelled) status("Scan stopped.", WARNING);
        else if (permissionProblem && records.isEmpty()) status("Android blocked local-network access. Check the app's Nearby devices permission in Settings.", DANGER);
        else {
            int current = 0;
            for (DeviceRecord record : records.values()) if (record.seenThisScan) current++;
            status("Scan complete: " + current + " connected device" + (current == 1 ? "" : "s") + ".", SUCCESS);
        }
    }

    private void cancelCurrentScan() {
        cancelScan.set(true);
        if (scanExecutor != null) scanExecutor.shutdownNow();
        if (scanRunning) status("Stopping scan…", WARNING);
    }

    private void shareReport() {
        StringBuilder report = new StringBuilder();
        report.append("Hotspot Devices report\n");
        report.append("Generated: ").append(DateFormat.getDateTimeInstance().format(new Date())).append('\n');
        report.append("Interface: ").append(selectedInterface == null ? "unknown" : selectedInterface).append('\n');
        report.append("Phone IP: ").append(ownAddress == null ? "unknown" : ownAddress).append('\n');
        report.append("Subnet: ").append(selectedCidr == null ? subnetInput.getText() : selectedCidr).append("\n\n");

        List<DeviceRecord> sorted = new ArrayList<>(records.values());
        sorted.sort(Comparator.comparingLong(record -> ipv4ToLong(record.ip)));
        for (DeviceRecord record : sorted) {
            report.append(record.seenThisScan ? "ONLINE  " : "SEEN    ")
                    .append(record.ip).append("  ")
                    .append(record.openPorts.isEmpty() ? "no common TCP ports" : formatPorts(record.openPorts))
                    .append("  last seen ")
                    .append(DateFormat.getDateTimeInstance().format(new Date(record.lastSeen))).append('\n');
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hotspot Devices report");
        intent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(intent, "Share device report"));
    }

    private void copyIp(String ip) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("IP address", ip));
        Toast.makeText(this, "Copied " + ip, Toast.LENGTH_SHORT).show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ex) {
            Toast.makeText(this, "No browser can open " + url, Toast.LENGTH_LONG).show();
        }
    }

    private String formatPorts(Set<Integer> ports) {
        List<String> parts = new ArrayList<>();
        for (int port : ports) parts.add(port + "/" + serviceName(port));
        return String.join("  •  ", parts);
    }

    private String serviceName(int port) {
        switch (port) {
            case 22: return "SSH";
            case 23: return "Telnet";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 443: return "HTTPS";
            case 554: return "RTSP";
            case 1883: return "MQTT";
            case 5000: return "HTTP-alt";
            case 8000: return "HTTP-alt";
            case 8080: return "HTTP-alt";
            case 8883: return "MQTT-TLS";
            case 8888: return "HTTP-alt";
            case 9100: return "Printer";
            default: return "TCP";
        }
    }

    private int firstPresent(Set<Integer> ports, int... candidates) {
        for (int candidate : candidates) if (ports.contains(candidate)) return candidate;
        return candidates[0];
    }

    private String time(long millis) {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(millis));
    }

    private void status(String message, int color) {
        statusText.setText(message);
        statusText.setTextColor(color);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private TextView label(String value) {
        TextView view = text(value, 12, ACCENT, true);
        view.setLetterSpacing(0.08f);
        return view;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(15), dp(14), dp(15), dp(14));
        layout.setBackground(roundRect(CARD, BORDER, 14));
        return layout;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(PRIMARY);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(roundRect(primary ? Color.rgb(22, 116, 209) : CARD, primary ? Color.rgb(22, 116, 209) : BORDER, 12));
        return button;
    }

    private Button smallButton(String value) {
        Button button = button(value, false);
        button.setTextSize(12);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        return button;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        long value = 0;
        for (String part : parts) value = (value << 8) | Integer.parseInt(part);
        return value;
    }

    private enum ProbeOutcome { OPEN, REFUSED, NO_RESPONSE, PERMISSION_DENIED }

    private static final class DeviceResult {
        final String ip;
        final Set<Integer> openPorts = new TreeSet<>();
        boolean alive;
        boolean permissionDenied;
        long latencyMs;
        DeviceResult(String ip) { this.ip = ip; }
    }

    private static final class DeviceRecord {
        final String ip;
        final long firstSeen;
        final Set<Integer> openPorts = new TreeSet<>();
        long lastSeen;
        long lastLatencyMs;
        boolean seenThisScan;
        DeviceRecord(String ip, long now) {
            this.ip = ip;
            this.firstSeen = now;
            this.lastSeen = now;
            this.seenThisScan = true;
        }
    }

    private static final class InterfaceCandidate {
        final String name;
        final String address;
        final int prefixLength;
        final String networkCidr;
        final int score;
        InterfaceCandidate(String name, String address, int prefixLength, String networkCidr, int score) {
            this.name = name;
            this.address = address;
            this.prefixLength = prefixLength;
            this.networkCidr = networkCidr;
            this.score = score;
        }
    }

    private static final class NetworkDetector {
        static List<InterfaceCandidate> findCandidates() {
            List<InterfaceCandidate> candidates = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                if (interfaces == null) return candidates;
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    String name = networkInterface.getName();
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;
                    if (lower.startsWith("rmnet") || lower.startsWith("ccmni") || lower.startsWith("pdp") || lower.startsWith("tun") || lower.startsWith("vpn")) continue;

                    networkInterface.getInterfaceAddresses().forEach(interfaceAddress -> {
                        InetAddress address = interfaceAddress.getAddress();
                        if (!(address instanceof Inet4Address)) return;
                        String ip = address.getHostAddress();
                        if (!isPrivate(ip) || address.isLoopbackAddress() || address.isLinkLocalAddress()) return;
                        int prefix = interfaceAddress.getNetworkPrefixLength();
                        if (prefix < 8 || prefix > 30) return;
                        int score = 0;
                        if (lower.contains("wlan")) score += 60;
                        if (lower.contains("ap") || lower.contains("softap") || lower.contains("swlan")) score += 50;
                        if (prefix == 24) score += 25;
                        if (prefix >= 22 && prefix <= 28) score += 10;
                        if (ip.startsWith("10.")) score += 8;
                        Subnet subnet = Subnet.fromAddress(ip, prefix);
                        candidates.add(new InterfaceCandidate(name, ip, prefix, subnet.cidr(), score));
                    });
                }
            } catch (Exception ignored) {
            }
            candidates.sort((a, b) -> Integer.compare(b.score, a.score));
            return candidates;
        }

        static boolean isPrivate(String ip) {
            long value = ipv4ToLong(ip);
            return (value >= ipv4ToLong("10.0.0.0") && value <= ipv4ToLong("10.255.255.255"))
                    || (value >= ipv4ToLong("172.16.0.0") && value <= ipv4ToLong("172.31.255.255"))
                    || (value >= ipv4ToLong("192.168.0.0") && value <= ipv4ToLong("192.168.255.255"));
        }
    }

    private static final class Subnet {
        final long network;
        final int prefix;
        final long mask;
        final long broadcast;

        private Subnet(long network, int prefix) {
            this.prefix = prefix;
            this.mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            this.network = network & mask;
            this.broadcast = this.network | (~mask & 0xFFFFFFFFL);
        }

        static Subnet parse(String input) {
            if (input == null || input.isEmpty()) throw new IllegalArgumentException("Enter a subnet, for example 10.20.167.0/24.");
            String[] parts = input.split("/");
            if (parts.length != 2) throw new IllegalArgumentException("Use CIDR notation, for example 10.20.167.0/24.");
            int prefix;
            try {
                prefix = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("The prefix after / must be a number.");
            }
            if (prefix < 8 || prefix > 30) throw new IllegalArgumentException("Use a prefix between /8 and /30. /24 is typical for a hotspot.");
            validateIpv4(parts[0]);
            return new Subnet(ipv4ToLong(parts[0]), prefix);
        }

        static Subnet fromAddress(String ip, int prefix) {
            return new Subnet(ipv4ToLong(ip), prefix);
        }

        String cidr() { return longToIpv4(network) + "/" + prefix; }

        int hostCount() {
            long count = Math.max(0, broadcast - network - 1);
            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        }

        List<String> hostAddresses() {
            List<String> hosts = new ArrayList<>();
            for (long value = network + 1; value < broadcast; value++) hosts.add(longToIpv4(value));
            return hosts;
        }

        private static void validateIpv4(String ip) {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) throw new IllegalArgumentException("Invalid IPv4 address.");
            for (String part : parts) {
                try {
                    int value = Integer.parseInt(part);
                    if (value < 0 || value > 255) throw new IllegalArgumentException("Invalid IPv4 address.");
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid IPv4 address.");
                }
            }
        }

        private static String longToIpv4(long value) {
            return ((value >> 24) & 255) + "." + ((value >> 16) & 255) + "." + ((value >> 8) & 255) + "." + (value & 255);
        }
    }
}
