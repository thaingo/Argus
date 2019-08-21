/*
 * Copyright (c) 2016, Salesforce.com, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
	 
package com.salesforce.dva.argus.service.metric.transform;

import com.salesforce.dva.argus.entity.Metric;
import com.salesforce.dva.argus.util.QueryContext;

import java.util.List;

/**
 * Culls metrics based on the matching of a regular expression against the metric name.<br>
 * <tt>EXCLUDE(&lt;expr&gt;, &lt;regex&gt;)</tt>
 *
 * @author  Ruofan Zhang(rzhang@salesforce.com)
 */
public class ExcludeTransformWrap implements Transform {

    //~ Methods **************************************************************************************************************************************

    @Override
    public List<Metric> transform(QueryContext context, List<Metric> metrics) {
        throw new UnsupportedOperationException("Exclude Transform cannot be performed without a regular expression.");
    }

    @Override
    public List<Metric> transform(QueryContext queryContext, List<Metric> metrics, List<String> constants) {
        List<Metric> removedMetrics = new IncludeTransform().transform(null, metrics, constants);

        try {
            metrics.removeAll(removedMetrics);
        } catch (Exception e) {
            throw new RuntimeException("Fail to remove some metrics for Exclude Transform!");
        }
        return metrics;
    }

    @Override
    public String getResultScopeName() {
        return TransformFactory.Function.EXCLUDE.name();
    }

    @Override
    public List<Metric> transform(QueryContext queryContext, List<Metric>... listOfList) {
        throw new UnsupportedOperationException("Exclude doesn't support multiple lists of metrics!");
    }
}
/* Copyright (c) 2016, Salesforce.com, Inc.  All rights reserved. */
