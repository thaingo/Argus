package com.salesforce.dva.argus.service.schema;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.salesforce.dva.argus.entity.MetricSchemaRecord;
import com.salesforce.dva.argus.service.SchemaService.RecordType;
import net.openhft.hashing.LongHashFunction;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.salesforce.dva.argus.entity.MetricSchemaRecord.DEFAULT_RETENTION_DISCOVERY_DAYS;
import static com.salesforce.dva.argus.entity.MetricSchemaRecord.EXPIRATION_TS;

public class MetricSchemaRecordList implements RecordFinder<MetricSchemaRecord> {
	private final static long ONE_DAY_IN_MILLIS = 24L * 3600L * 1000L;
	private Map<String, MetricSchemaRecord> _idToSchemaRecordMap = new HashMap<>();
	private String _scrollID;
	private long totalHits;
	
	public MetricSchemaRecordList(Set<MetricSchemaRecord> records, HashAlgorithm algorithm) {
		for(MetricSchemaRecord record : records) {
			String id = null;
			if(HashAlgorithm.MD5.equals(algorithm)) {
				id = DigestUtils.md5Hex(MetricSchemaRecord.print(record));
			} else {
				id = String.valueOf(LongHashFunction.xx().hashChars(MetricSchemaRecord.print(record)));
			}
			_idToSchemaRecordMap.put(id, record);
		}
	}
	
	private MetricSchemaRecordList(List<MetricSchemaRecord> records, String scrollID, long totalHits) {
		int count = 0;
		for(MetricSchemaRecord record : records) {
			_idToSchemaRecordMap.put(String.valueOf(count++), record);
		}
		setScrollID(scrollID);
		this.totalHits = totalHits;
	}

	@Override
	public List<MetricSchemaRecord> getRecords() {
		return new ArrayList<>(_idToSchemaRecordMap.values());
	}

	@Override
	public Set<String> getIdSet() {
		return _idToSchemaRecordMap.keySet();
	}
	
	@Override
	public String getScrollID() {
		return _scrollID;
	}

	@Override
	public void setScrollID(String scrollID) {
		this._scrollID = scrollID;
	}
	
	@Override
	public MetricSchemaRecord getRecord(String id) {
		return _idToSchemaRecordMap.get(id);
	}

	public long getTotalHits() {
		return totalHits;
	}

	public enum HashAlgorithm {
		MD5,
		XXHASH;
		
		public static HashAlgorithm fromString(String str) throws IllegalArgumentException {
			for(HashAlgorithm algo : HashAlgorithm.values()) {
				if(algo.name().equalsIgnoreCase(str)) {
					return algo;
				}
			}
			
			throw new IllegalArgumentException(str + " does not match any of the available algorithms.");
		}
	}

	/* these two serializers are no longer used
	static class CreateSerializer extends JsonSerializer<MetricSchemaRecordList> {

		@Override
		public void serialize(MetricSchemaRecordList list, JsonGenerator jgen, SerializerProvider provider)
				throws IOException{

			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			final long now = System.currentTimeMillis();

			for(Map.Entry<String, MetricSchemaRecord> entry : list._idToSchemaRecordMap.entrySet()) {
				jgen.writeRaw("{ \"create\" : {\"_id\" : \"" + entry.getKey() + "\"}}");
				jgen.writeRaw(System.lineSeparator());
				String fieldsData = mapper.writeValueAsString(entry.getValue());

				String mtsField = "\"mts\":" + now;
				String ctsField = "\"cts\":" + now;
				Integer retention = entry.getValue().getRetentionDiscovery();
				Long expiration = now + (retention==null? DEFAULT_RETENTION_DISCOVERY_DAYS:retention) * ONE_DAY_IN_MILLIS;
				String expirationField = "\"" + EXPIRATION_TS + "\":" + expiration;

				jgen.writeRaw(fieldsData.substring(0, fieldsData.length()-1) + "," + mtsField + "," + ctsField + "," + expirationField + "}");
				jgen.writeRaw(System.lineSeparator());
			}
		}
    }

	static class UpdateSerializer extends JsonSerializer<MetricSchemaRecordList> {

		@Override
		public void serialize(MetricSchemaRecordList list, JsonGenerator jgen, SerializerProvider provider)
				throws IOException {

			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			final long now = System.currentTimeMillis();

			for(Map.Entry<String, MetricSchemaRecord> entry : list._idToSchemaRecordMap.entrySet()) {
				jgen.writeRaw("{\"update\":{\"_id\":\"" + entry.getKey() + "\"}}");
				jgen.writeRaw(System.lineSeparator());

				StringBuilder updateSB = new StringBuilder();
				updateSB.append("\"mts\":" + now);
				Integer retention = entry.getValue().getRetentionDiscovery();
				if (retention != null) {
					updateSB.append(",\"").append(RETENTION_DISCOVERY).append("\":").append(retention);
					updateSB.append(",\"").append(EXPIRATION_TS).append("\":").append(now + ONE_DAY_IN_MILLIS * retention);
				} else {
					updateSB.append(",\"").append(EXPIRATION_TS).append("\":").append(now + ONE_DAY_IN_MILLIS * DEFAULT_RETENTION_DISCOVERY_DAYS);

				}
				jgen.writeRaw("{\"doc\":{" + updateSB + "}}");
				jgen.writeRaw(System.lineSeparator());
			}
		}
	} */

	/**
	 * serialize to json string used for a bulk INDEX call
	 */
	static class IndexSerializer extends JsonSerializer<MetricSchemaRecordList> {

		@Override
		public void serialize(MetricSchemaRecordList list, JsonGenerator jgen, SerializerProvider provider)
				throws IOException{

			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(Include.NON_NULL);
			final long now = System.currentTimeMillis();

			for(Map.Entry<String, MetricSchemaRecord> entry : list._idToSchemaRecordMap.entrySet()) {
				jgen.writeRaw("{ \"index\" : {\"_id\" : \"" + entry.getKey() + "\"}}");
				jgen.writeRaw(System.lineSeparator());
				String fieldsData = mapper.writeValueAsString(entry.getValue());

				String mtsField = "\"mts\":" + now;
				String ctsField = "\"cts\":" + now;	//there might be no point for cts now since it will always = mts
				Integer retention = entry.getValue().getRetentionDiscovery();
				Long expiration = now + (retention==null? DEFAULT_RETENTION_DISCOVERY_DAYS:retention) * ONE_DAY_IN_MILLIS;
				String expirationField = "\"" + EXPIRATION_TS + "\":" + expiration;

				jgen.writeRaw(fieldsData.substring(0, fieldsData.length()-1) + "," + mtsField + "," + ctsField + "," + expirationField + "}");
				jgen.writeRaw(System.lineSeparator());
			}
		}
	}

	static class Deserializer extends JsonDeserializer<MetricSchemaRecordList> {

		@Override
		public MetricSchemaRecordList deserialize(JsonParser jp, DeserializationContext context)
				throws IOException, JsonProcessingException {
			
			String scrollID = null;
			List<MetricSchemaRecord> records = Collections.emptyList();
			
			JsonNode rootNode = jp.getCodec().readTree(jp);
			if(rootNode.has("_scroll_id")) {
				scrollID = rootNode.get("_scroll_id").asText();
			}
			long totalHits = rootNode.get("hits").get("total").asLong();
			JsonNode hits = rootNode.get("hits").get("hits");
			
			if(JsonNodeType.ARRAY.equals(hits.getNodeType())) {
				records = new ArrayList<>(hits.size());
				Iterator<JsonNode> iter = hits.elements();
				while(iter.hasNext()) {
					JsonNode hit = iter.next();
					JsonNode source = hit.get("_source");

					JsonNode namespaceNode = source.get(RecordType.NAMESPACE.getName());
					JsonNode scopeNode = source.get(RecordType.SCOPE.getName());
					JsonNode metricNode = source.get(RecordType.METRIC.getName());
					JsonNode tagkNode = source.get(RecordType.TAGK.getName());
					JsonNode tagvNode = source.get(RecordType.TAGV.getName());
					JsonNode retentionNode = source.get(RecordType.RETENTION_DISCOVERY.getName());
					
					records.add(new MetricSchemaRecord(namespaceNode == null ? null : namespaceNode.asText(), 
													   	scopeNode.asText(),
													   	metricNode.asText(),
													   	tagkNode == null ? null : tagkNode.asText(),
													   	tagvNode == null ? null : tagvNode.asText(),
														retentionNode == null? null : retentionNode.asInt()));
				}
			}
			
			return new MetricSchemaRecordList(records, scrollID, totalHits);
		}
		
	}
}
