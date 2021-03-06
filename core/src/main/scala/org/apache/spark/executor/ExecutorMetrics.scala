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
package org.apache.spark.executor

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.metrics.ExecutorMetricType

/**
 * :: DeveloperApi ::
 * Metrics tracked for executors and the driver.
 *
 * Executor-level metrics are sent from each executor to the driver as part of the Heartbeat.
 */
@DeveloperApi
class ExecutorMetrics private[spark] extends Serializable {

  // Metrics are indexed by MetricGetter.values
  private val metrics = new Array[Long](ExecutorMetricType.values.length)

  // the first element is initialized to -1, indicating that the values for the array
  // haven't been set yet.
  metrics(0) = -1

  /** Returns the value for the specified metricType. */
  def getMetricValue(metricType: ExecutorMetricType): Long = {
    metrics(ExecutorMetricType.metricIdxMap(metricType))
  }

  /** Returns true if the values for the metrics have been set, false otherwise. */
  def isSet(): Boolean = metrics(0) > -1

  private[spark] def this(metrics: Array[Long]) {
    this()
    Array.copy(metrics, 0, this.metrics, 0, Math.min(metrics.size, this.metrics.size))
  }

  /**
   * Constructor: create the ExecutorMetrics with the values specified.
   *
   * @param executorMetrics map of executor metric name to value
   */
  private[spark] def this(executorMetrics: Map[String, Long]) {
    this()
    (0 until ExecutorMetricType.values.length).foreach { idx =>
      metrics(idx) = executorMetrics.getOrElse(ExecutorMetricType.values(idx).name, 0L)
    }
  }

  /**
   * Compare the specified executor metrics values with the current executor metric values,
   * and update the value for any metrics where the new value for the metric is larger.
   *
   * @param executorMetrics the executor metrics to compare
   * @return if there is a new peak value for any metric
   */
  private[spark] def compareAndUpdatePeakValues(executorMetrics: ExecutorMetrics): Boolean = {
    var updated = false

    (0 until ExecutorMetricType.values.length).foreach { idx =>
       if (executorMetrics.metrics(idx) > metrics(idx)) {
        updated = true
        metrics(idx) = executorMetrics.metrics(idx)
      }
    }
    updated
  }
}
