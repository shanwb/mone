package com.xiaomi.youpin.prometheus.client.multi;

import com.xiaomi.youpin.prometheus.client.MetricsManager;
import com.xiaomi.youpin.prometheus.client.PrometheusCounter;
import com.xiaomi.youpin.prometheus.client.PrometheusGauge;
import com.xiaomi.youpin.prometheus.client.PrometheusHistogram;
import com.xiaomi.youpin.prometheus.client.XmCounter;
import com.xiaomi.youpin.prometheus.client.XmGauge;
import com.xiaomi.youpin.prometheus.client.XmHistogram;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zhangxiaowei
 */
@Slf4j
public class MutiPrometheus implements MetricsManager {

    public static final int CONST_LABELS_NUM = 2;
    private static final int SEGMENT_COUNT = 7;
    private Map<String, String> constLabels;
    private Map<String, Object> prometheusMetrics;
    private Map<String, Object> prometheusTypeMetrics;

    private CollectorRegistry registry;

    private final ReentrantLock[] segmentLocks = new ReentrantLock[SEGMENT_COUNT];

    public MutiPrometheus() {
        this.prometheusMetrics = new ConcurrentHashMap<>();
        this.prometheusTypeMetrics = new ConcurrentHashMap<>();
        initSegmentLocks();
    }

    public MutiPrometheus(CollectorRegistry registry) {
        this.prometheusMetrics = new ConcurrentHashMap<>();
        this.prometheusTypeMetrics = new ConcurrentHashMap<>();
        this.registry = registry;
        initSegmentLocks();
    }

    private void initSegmentLocks() {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            segmentLocks[i] = new ReentrantLock();
        }
    }

    /**
     * 根据metricName获取对应的分段锁
     */
    private ReentrantLock getSegmentLock(String metricName) {
        int hash = metricName.hashCode();
        // 使用绝对值避免负数，然后取模
        int index = Math.abs(hash) % SEGMENT_COUNT;
        return segmentLocks[index];
    }

    public Map<String, String> getConstLabels() {
        return this.constLabels;
    }

    public Map<String, String> setConstLabels(Map<String, String> constLabels) {
        this.constLabels = constLabels;
        return this.constLabels;
    }

    public Map<String, Object> getPrometheusMetrics() {
        return this.prometheusMetrics;
    }

    public Map<String, Object> getPrometheusTypeMetrics() {
        return prometheusTypeMetrics;
    }

    @Override
    public XmCounter newCounter(String metricName, String... labelName) {
        // 🚀 快速检查：大部分情况下直接返回
        XmCounter existing = (XmCounter) prometheusTypeMetrics.get(metricName);
        if (existing != null) {
            return existing;
        }
        
        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            // 🔒 锁内双重检查
            existing = (XmCounter) prometheusTypeMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }

            Counter counter = createCounterUnsafe(metricName, labelName);
            if (counter == null) {
                return null;
            }

            PrometheusCounter prometheusCounter = new PrometheusCounter(counter, labelName, null, this);
            prometheusTypeMetrics.put(metricName, prometheusCounter);
            return prometheusCounter;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public XmGauge newGauge(String metricName, String... labelName) {
        // 🚀 快速检查：大部分情况下直接返回
        XmGauge existing = (XmGauge) prometheusTypeMetrics.get(metricName);
        if (existing != null) {
            return existing;
        }

        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            // 🔒 锁内双重检查
            existing = (XmGauge) prometheusTypeMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }
            
            Gauge gauge = createGaugeUnsafe(metricName, labelName);
            if (gauge == null) {
                return null;
            }
            
            PrometheusGauge prometheusGauge = new PrometheusGauge(gauge, labelName, null, this);
            prometheusTypeMetrics.put(metricName, prometheusGauge);
            return prometheusGauge;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public XmHistogram newHistogram(String metricName, double[] bucket, String... labelNames) {
        // 🚀 快速检查：大部分情况下直接返回，避免锁竞争
        XmHistogram existing = (XmHistogram) prometheusTypeMetrics.get(metricName);
        if (existing != null) {
            return existing;
        }
        
        // 🚀 锁外预准备：耗时操作移到锁外
        if (constLabels.size() != MutiPrometheus.CONST_LABELS_NUM) {
            return null;
        }
        
        // 🚀 锁外准备数据
        List<String> mylist = new ArrayList<>(Arrays.asList(labelNames));
        String[] preparedLabelNames = mylist.toArray(new String[mylist.size()]);
        String preparedNamespace = constLabels.get(MutiMetrics.GROUP) + 
                ("".equals(constLabels.get(MutiMetrics.SERVICE)) ? "" : "_" + constLabels.get(MutiMetrics.SERVICE));

        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            // 🔒 锁内双重检查，避免竞态条件
            existing = (XmHistogram) prometheusTypeMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }
            
            // 🔒 锁内创建Histogram（使用预准备的数据）
            Histogram histogram = createHistogramOptimized(metricName, bucket, preparedLabelNames, preparedNamespace);
            if (histogram == null) {
                return null;
            }
            
            // 🔒 锁内更新缓存
            PrometheusHistogram prometheusHistogram = new PrometheusHistogram(histogram, labelNames, null, this);
            prometheusTypeMetrics.put(metricName, prometheusHistogram);
            return prometheusHistogram;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 创建Counter的内部方法，调用前必须已经获取对应的分段锁
     */
    private Counter createCounterUnsafe(String metricName, String... labelName) {
        if (constLabels.size() != MutiPrometheus.CONST_LABELS_NUM) {
            return null;
        }
        try {
            // 检查是否存在（调用方已经加锁）
            Counter existing = (Counter) prometheusMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }

            // register if not exist
            List<String> mylist = new ArrayList<>(Arrays.asList(labelName));
            String[] finalValue = mylist.toArray(new String[mylist.size()]);
            Counter newCounter = Counter.build()
                    .name(metricName)
                    .namespace(constLabels.get(MutiMetrics.GROUP) + ("".equals(constLabels.get(MutiMetrics.SERVICE)) ? "" : "_" + constLabels.get(MutiMetrics.SERVICE)))
                            .labelNames(finalValue)
                            .help(metricName)
                            .register(registry);

            prometheusMetrics.put(metricName, newCounter);
            return newCounter;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    /**
     * 创建Gauge的内部方法，调用前必须已经获取对应的分段锁
     */
    private Gauge createGaugeUnsafe(String metricName, String... labelName) {
        if (constLabels.size() != MutiPrometheus.CONST_LABELS_NUM) {
            return null;
        }
        try {
            // 检查是否存在（调用方已经加锁）
            Gauge existing = (Gauge) prometheusMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }

            // register if not exist
            List<String> mylist = new ArrayList<>(Arrays.asList(labelName));
            String[] finalValue = mylist.toArray(new String[mylist.size()]);
            Gauge newGauge = Gauge.build()
                    .name(metricName)
                    .namespace(constLabels.get(MutiMetrics.GROUP) + ("".equals(constLabels.get(MutiMetrics.SERVICE)) ? "" : "_" + constLabels.get(MutiMetrics.SERVICE)))
                    .labelNames(finalValue)
                    .help(metricName)
                    .register(registry);

            prometheusMetrics.put(metricName, newGauge);
            return newGauge;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    /**
     * 优化的Histogram创建方法，使用预准备数据，调用前必须已加锁
     */
    private Histogram createHistogramOptimized(String metricName, double[] buckets, String[] preparedLabelNames, String preparedNamespace) {
        try {
            // 快速检查（调用方已加锁）
            Histogram existing = (Histogram) prometheusMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }

            // 使用预准备的数据创建Histogram
            Histogram newHistogram = Histogram.build()
                    .buckets(buckets)
                    .name(metricName)
                    .namespace(preparedNamespace)
                    .labelNames(preparedLabelNames)
                    .help(metricName)
                    .register(registry);
                    
            prometheusMetrics.put(metricName, newHistogram);
            return newHistogram;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    /**
     * 创建Histogram的内部方法，调用前必须已经获取对应的分段锁
     */
    private Histogram createHistogramUnsafe(String metricName, double[] buckets, String... labelNames) {
        if (constLabels.size() != MutiPrometheus.CONST_LABELS_NUM) {
            return null;
        }
        try {
            // 检查是否存在（调用方已经加锁）
            Histogram existing = (Histogram) prometheusMetrics.get(metricName);
            if (existing != null) {
                return existing;
            }

            // register if not exist
            List<String> mylist = new ArrayList<>(Arrays.asList(labelNames));
            String[] finalValue = mylist.toArray(new String[mylist.size()]);
            Histogram newHistogram = Histogram.build()
                    .buckets(buckets)
                    .name(metricName)
                    .namespace(constLabels.get(MutiMetrics.GROUP) + ("".equals(constLabels.get(MutiMetrics.SERVICE)) ? "" : "_" + constLabels.get(MutiMetrics.SERVICE)))
                    .labelNames(finalValue)
                    .help(metricName)
                    .register(registry);
            prometheusMetrics.put(metricName, newHistogram);
            return newHistogram;
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    public Counter getCounter(String metricName, String... labelName) {
        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            return createCounterUnsafe(metricName, labelName);
        } finally {
            lock.unlock();
        }
    }

    public Gauge getGauge(String metricName, String... labelName) {
        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            return createGaugeUnsafe(metricName, labelName);
        } finally {
            lock.unlock();
        }
    }

    public Histogram getHistogram(String metricName, double[] buckets, String... labelNames) {
        ReentrantLock lock = getSegmentLock(metricName);
        lock.lock();
        try {
            return createHistogramUnsafe(metricName, buckets, labelNames);
        } finally {
            lock.unlock();
        }
    }
}
