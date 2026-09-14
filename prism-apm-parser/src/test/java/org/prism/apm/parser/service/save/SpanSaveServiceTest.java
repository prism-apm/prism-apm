package org.prism.apm.parser.service.save;


import org.prism.apm.parser.cache.CacheService;
import org.prism.apm.parser.mapper.ServiceCallEntityMapper;
import org.prism.apm.parser.model.span.SpanInfo;
import org.prism.apm.parser.model.entity.ServiceCallEntity;
import org.prism.apm.parser.repository.ServiceCallMapper;
import org.prism.apm.parser.service.save.bo.SpanSaveBo;
import org.prism.apm.parser.service.save.bo.SpanSaveOtherProcessor;
import org.prism.apm.parser.service.save.bo.SpanSaveProcessor;
import org.prism.apm.parser.service.save.bo.SpanSaveServerProcessor;
import org.prism.apm.parser.utils.CommonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


/**
 * @author john
 * @date 2024-09-05 15:44
 */
@ExtendWith(MockitoExtension.class)
public class SpanSaveServiceTest {

    // TODO: Update this class so it can test with the new parser.
    // TODO: Rewrite this test class. The original test coverage has moved into integration tests.

//    @Mock
//    private CacheService cacheService;
//
//    @Spy
//    private ServiceCallMapper svcCallMapper;
//
//    @InjectMocks
//    private SpanSaveService spanSaveService;
//
//
//    private SpanInfo caller1SpanInfo;
//    private SpanInfo callee1SpanInfo;
//    private SpanInfo caller2SpanInfo;
//    private SpanInfo callee2SpanInfo;
//
//    private SpanInfo multiServiceCallerServerSpanInfo;
//    private SpanInfo multiServiceCallerClient1SpanInfo;
//    private SpanInfo multiServiceCallerClient2SpanInfo;
//    private SpanInfo multiServiceCalleeServerSpanInfo;
//    private SpanInfo multiServiceCalleeClientSpanInfo;
//
//
//
//    @Before
//    public void setUp() {
//        // prepare span data
//        SpanParser spanParser = new SpanParser(new SpanInfoBuilderFactory());
//
//        SpanJsonObj caller1JsonObj = spanParser.convertToObj(callerSpan1);
//        SpanJsonObj callee1JsonObj = spanParser.convertToObj(calleeSpan1);
//        caller1SpanInfo = spanParser.parse(caller1JsonObj);
//        callee1SpanInfo = spanParser.parse(callee1JsonObj);
//
//
//
//        SpanJsonObj caller2JsonObj = spanParser.convertToObj(callerSpan2);
//        SpanJsonObj callee2JsonObj = spanParser.convertToObj(calleeSpan2);
//        caller2SpanInfo = spanParser.parse(caller2JsonObj);
//        callee2SpanInfo = spanParser.parse(callee2JsonObj);
//
//
//
//        SpanJsonObj multiServiceCallerServerJsonObj = spanParser.convertToObj(multiService1CallerServiceServerSpan);
//        SpanJsonObj multiServiceCallerClient1JsonObj = spanParser.convertToObj(multiService1CallerServiceClient1Span);
//        SpanJsonObj multiServiceCallerClient2JsonObj = spanParser.convertToObj(multiService1CallerServiceClient2Span);
//        SpanJsonObj multiServiceCalleeServerJsonObj = spanParser.convertToObj(multiService1CalleeServiceServerSpan);
//        SpanJsonObj multiServiceCalleeClient1JsonObj = spanParser.convertToObj(multiService1CalleeServiceClientSpan);
//
//        multiServiceCallerServerSpanInfo = spanParser.parse(multiServiceCallerServerJsonObj);
//        multiServiceCallerClient1SpanInfo = spanParser.parse(multiServiceCallerClient1JsonObj);
//        multiServiceCallerClient2SpanInfo = spanParser.parse(multiServiceCallerClient2JsonObj);
//        multiServiceCalleeServerSpanInfo = spanParser.parse(multiServiceCalleeServerJsonObj);
//        multiServiceCalleeClientSpanInfo = spanParser.parse(multiServiceCalleeClient1JsonObj);
//
//        // prepare SpanSaveProcessor
//        SpanSaveBo spanSaveBo = new SpanSaveBo();
//        SpanSaveProcessor spanSaveProcessor = new SpanSaveProcessor(
//                spanSaveBo, new SpanSaveServerProcessor(spanSaveBo), new SpanSaveOtherProcessor(spanSaveBo), TimeUnit.SECONDS.toMillis(600));
//        spanSaveService.setSpanSaveProcessor(spanSaveProcessor);
//
//    }
//
//    /********************************************************/
//    /*********** one spans param testcases start *************/
//    /********************************************************/
//    @Test
//    public void testHandleSpansSuccess_oneSpanParam_withCallerExistAndHandleCallee() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(callee1SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(caller1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller1SpanInfo));
//        when(cacheService.get(callee1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee1SpanInfo));
//
//
//        // call test method
//        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//        // Capture the argument passed to svcCallMapper.batchInsert
//        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//        verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//        // Verify that the inserted ServiceCallEntity list contains the expected data
//        List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//        assertEquals(1, capturedEntities.size());
//
//        ServiceCallEntity capturedEntity = capturedEntities.get(0);
//        checkEntityWithParentSpan(caller1SpanInfo, callee1SpanInfo, capturedEntity);
//
//        // Verify return value
//        assertTrue(delayedProcessingSpanIds.isEmpty());
//    }
//
//    @Test
//    public void testHandleSpansSuccess_oneSpanParam_withMissingCallerSpanAndTimeout() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(callee1SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(callee1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee1SpanInfo));
//
//
//        try (MockedStatic<CommonUtil> mockedStatic = mockStatic(CommonUtil.class, Mockito.CALLS_REAL_METHODS)) {
//            // let span time out.
//
//            mockedStatic.when(CommonUtil::getCurrentTimeMillis).thenReturn(2725608478491L);// 2056-05-15 17:27:58
//
//            // call test method
//            List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//            // Capture the argument passed to svcCallMapper.batchInsert
//            ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//            verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//            // Verify that the inserted ServiceCallEntity list contains the expected data
//            List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//            assertEquals(1, capturedEntities.size());
//
//            checkEntityWithoutParentSpan(callee1SpanInfo, capturedEntities.get(0));
//
//            // Verify return value
//            assertTrue(delayedProcessingSpanIds.isEmpty());
//        }
//    }
//
//    @Test
//    public void testHandleSpansSuccess_oneSpanParam_withMissingCallerSpanAndWithoutTimeout() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(callee1SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(callee1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee1SpanInfo));
//
//
//        try (MockedStatic<CommonUtil> mockedStatic = mockStatic(CommonUtil.class, Mockito.CALLS_REAL_METHODS)) {
//            // let span time out.
//
//            mockedStatic.when(CommonUtil::getCurrentTimeMillis).thenReturn(0L);
//
//            // call test method
//            List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//            //
//            verify(svcCallMapper, never()).batchInsert(null);
//
//            // Verify return value
//            assertEquals(delayedProcessingSpanIds.size(), 1);
//            assertEquals(delayedProcessingSpanIds.get(0), callee1SpanInfo.getSpanId());
//        }
//    }
//
//    /********************************************************/
//    /*********** one spans param testcases end **************/
//    /********************************************************/
//
//    /********************************************************/
//    /*********** two spans param testcases start *************/
//    /********************************************************/
//
//    /**
//     * two spans:
//     * - caller span
//     * - callee span
//     */
//    @Test
//    public void testHandleSpansSuccess_twoSpanParam_withCallerExist() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(caller1SpanInfo.getSpanId());
//        spanIdList.add(callee1SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(caller1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller1SpanInfo));
//        when(cacheService.get(callee1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee1SpanInfo));
//
//
//        // call test method
//        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//        // Capture the argument passed to svcCallMapper.batchInsert
//        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//        verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//        // Verify that the inserted ServiceCallEntity list contains the expected data
//        List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//        assertEquals(2, capturedEntities.size());
//
//
//        checkEntityWithoutParentSpan(caller1SpanInfo, capturedEntities.get(0));
//        checkEntityWithParentSpan(caller1SpanInfo, callee1SpanInfo, capturedEntities.get(1));
//
//
//        // Verify return value
//        assertTrue(delayedProcessingSpanIds.isEmpty());
//    }
//
//    /**
//     * twoSpanParam: two callee spans
//     */
//    @Test
//    public void testHandleSpansSuccess_twoSpanParam_withCallerExist1() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(callee1SpanInfo.getSpanId());
//        spanIdList.add(callee2SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(caller1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller1SpanInfo));
//        when(cacheService.get(callee1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee1SpanInfo));
//        when(cacheService.get(caller2SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller2SpanInfo));
//        when(cacheService.get(callee2SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(callee2SpanInfo));
//
//
//        // call test method
//        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//        // Capture the argument passed to svcCallMapper.batchInsert
//        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//        verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//        // Verify that the inserted ServiceCallEntity list contains the expected data
//        List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//        assertEquals(2, capturedEntities.size());
//
//
//        checkEntityWithParentSpan(caller1SpanInfo, callee1SpanInfo, capturedEntities.get(0));
//        checkEntityWithParentSpan(caller2SpanInfo, callee2SpanInfo, capturedEntities.get(1));
//
//
//        // Verify return value
//        assertTrue(delayedProcessingSpanIds.isEmpty());
//    }
//
//    /**
//     * twoSpanParam: two caller spans
//     */
//    @Test
//    public void testHandleSpansSuccess_twoSpanParam_withCallerExist2() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(caller1SpanInfo.getSpanId());
//        spanIdList.add(caller2SpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(caller1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller1SpanInfo));
//        when(cacheService.get(caller2SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(caller2SpanInfo));
//
//
//        // call test method
//        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//        // Capture the argument passed to svcCallMapper.batchInsert
//        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//        verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//        // Verify that the inserted ServiceCallEntity list contains the expected data
//        List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//        assertEquals(2, capturedEntities.size());
//
//
//        checkEntityWithoutParentSpan(caller1SpanInfo, capturedEntities.get(0));
//        checkEntityWithoutParentSpan(caller2SpanInfo, capturedEntities.get(1));
//
//
//        // Verify return value
//        assertTrue(delayedProcessingSpanIds.isEmpty());
//    }
//
//    /********************************************************/
//    /*********** two spans param testcases end **************/
//    /********************************************************/
//
//
//
//    /************************************************************/
//    /*********** multi service call testcases start *************/
//    /************************************************************/
//
//    @Test
//    public void testHandleSpansSuccess_multiService() {
//        List<String> spanIdList = new ArrayList<>();
//
//        spanIdList.add(multiServiceCalleeClientSpanInfo.getSpanId());
//        spanIdList.add(multiServiceCalleeServerSpanInfo.getSpanId());
//        spanIdList.add(multiServiceCallerClient2SpanInfo.getSpanId());
//        spanIdList.add(multiServiceCallerClient1SpanInfo.getSpanId());
//        spanIdList.add(multiServiceCallerServerSpanInfo.getSpanId());
//
//        // Mock CacheService behavior
//        when(cacheService.get(multiServiceCalleeClientSpanInfo.getSpanId())).thenReturn(JSON.toJSONString(multiServiceCalleeClientSpanInfo));
//        when(cacheService.get(multiServiceCalleeServerSpanInfo.getSpanId())).thenReturn(JSON.toJSONString(multiServiceCalleeServerSpanInfo));
//        when(cacheService.get(multiServiceCallerClient2SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(multiServiceCallerClient2SpanInfo));
//        when(cacheService.get(multiServiceCallerClient1SpanInfo.getSpanId())).thenReturn(JSON.toJSONString(multiServiceCallerClient1SpanInfo));
//        when(cacheService.get(multiServiceCallerServerSpanInfo.getSpanId())).thenReturn(JSON.toJSONString(multiServiceCallerServerSpanInfo));
//
//
//        // call test method
//        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList, false);
//
//
//        // Capture the argument passed to svcCallMapper.batchInsert
//        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
//        verify(svcCallMapper, times(1)).batchInsert(argumentCaptor.capture());
//
//        // Verify that the inserted ServiceCallEntity list contains the expected data
//        List<ServiceCallEntity> capturedEntities = argumentCaptor.getValue();
//        assertEquals(4, capturedEntities.size());
//
//
//        checkEntityWithParentSpan(multiServiceCalleeServerSpanInfo,
//                multiServiceCalleeClientSpanInfo,
//                capturedEntities.get(0));
//        checkEntityWithParentSpan(multiServiceCallerClient2SpanInfo,
//                multiServiceCalleeServerSpanInfo,
//                capturedEntities.get(1));
//        checkEntityWithParentSpan(multiServiceCallerServerSpanInfo,
//                multiServiceCallerClient1SpanInfo,
//                capturedEntities.get(2));
//        checkEntityWithoutParentSpan(multiServiceCallerServerSpanInfo, capturedEntities.get(3));
//
//
//        // Verify return value
//        assertTrue(delayedProcessingSpanIds.isEmpty());
//    }
//
//
//    /**********************************************************/
//    /*********** multi service call testcases end *************/
//    /**********************************************************/
//
//    private void checkEntityWithoutParentSpan(SpanInfo calleeSpanInfo, ServiceCallEntity callerEntity) {
//
//        assertEquals(calleeSpanInfo.getServiceName(), callerEntity.getServiceName());
//        assertEquals(calleeSpanInfo.getHostName(), callerEntity.getHostName());
//        assertEquals(calleeSpanInfo.getComponent(), callerEntity.getComponent());
//        assertEquals(calleeSpanInfo.getSpanKind(), callerEntity.getSpanKind());
//        assertEquals(calleeSpanInfo.getActionName(), callerEntity.getActionName());
//        assertEquals(calleeSpanInfo.getTime().longValue(),
//                CommonUtil.convertOffsetDateTimeToEpoch(callerEntity.getTime()));
//        assertEquals(calleeSpanInfo.getDuration(), callerEntity.getDuration());
//
//        // Verify caller-related fields
//        assertEquals(ServiceCallEntityMapper.TEXT_DEFAULT_VALUE, callerEntity.getCallerServiceName());
//        assertEquals(ServiceCallEntityMapper.TEXT_DEFAULT_VALUE, callerEntity.getCallerHostName());
//        assertEquals(ServiceCallEntityMapper.TEXT_DEFAULT_VALUE, callerEntity.getCallerActionName());
//        assertEquals(ServiceCallEntityMapper.TEXT_DEFAULT_VALUE, callerEntity.getCallerComponent());
//        assertEquals(ServiceCallEntityMapper.TEXT_DEFAULT_VALUE, callerEntity.getCallerSpanKind());
//        assertEquals(CommonUtil.convertEpochToOffsetDateTime(0),
//                callerEntity.getCallerTime());
//
//    }
//
//    private void checkEntityWithParentSpan(SpanInfo callerSpanInfo, SpanInfo calleeSpanInfo, ServiceCallEntity calleeEntity) {
//        assertEquals(calleeSpanInfo.getServiceName(), calleeEntity.getServiceName());
//        assertEquals(calleeSpanInfo.getHostName(), calleeEntity.getHostName());
//        assertEquals(calleeSpanInfo.getComponent(), calleeEntity.getComponent());
//        assertEquals(calleeSpanInfo.getSpanKind(), calleeEntity.getSpanKind());
//        assertEquals(calleeSpanInfo.getActionName(), calleeEntity.getActionName());
//        assertEquals(calleeSpanInfo.getTime().longValue(),
//                CommonUtil.convertOffsetDateTimeToEpoch(calleeEntity.getTime()));
//        assertEquals(calleeSpanInfo.getDuration(), calleeEntity.getDuration());
//
//        assertEquals(callerSpanInfo.getServiceName(), calleeEntity.getCallerServiceName());
//        assertEquals(callerSpanInfo.getHostName(), calleeEntity.getCallerHostName());
//        assertEquals(callerSpanInfo.getActionName(), calleeEntity.getCallerActionName());
//        assertEquals(callerSpanInfo.getComponent(), calleeEntity.getCallerComponent());
//        assertEquals(callerSpanInfo.getSpanKind(), calleeEntity.getCallerSpanKind());
//        assertEquals(CommonUtil.convertEpochToOffsetDateTime(callerSpanInfo.getTime()),
//                calleeEntity.getCallerTime());
//    }
//
//
//
////    @Test
////    public void testHandleSpansTimeout() {
////        // Simulate span timeout
////        when(spanSaveBo.isTimeout(any(SpanInfo.class), anyLong(), anyLong())).thenReturn(true);
////
////        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList);
////
////        // No processing should occur if the span has timed out
////        verify(spanSaveProcessor, times(0)).process(any(), any(), anyList(), anyList());
////        verify(svcCallMapper, times(0)).batchInsert(anyList());
////
////        assertTrue(delayedProcessingSpanIds.isEmpty());
////    }
////
////    @Test
////    public void testHandleSpansWithDelayedProcessing() {
////        // Simulate the case where the caller span is missing, thus the span needs delayed processing
////        when(cacheService.get(childSpanId)).thenReturn(null);
////
////        List<String> delayedProcessingSpanIds = spanSaveService.handle(spanIdList);
////
////        // The child span should be added to the delayed processing list
////        assertEquals(1, delayedProcessingSpanIds.size());
////        assertEquals(childSpanId, delayedProcessingSpanIds.get(0));
////    }
//
//
//    /**
//     *
//     * {"traceId":"AAAAAAAAAAAblNgNeqlniQ==","spanId":"G5TYDXqpZ4k=","operationName":"searchRdb","flags":1,"startTime":"2024-09-05T07:40:23.973Z","duration":"0.014859s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"component","vStr":"java-web-servlet"},{"key":"span.kind","vStr":"server"},{"key":"sampler.type","vStr":"const"},{"key":"sampler.param","vType":"BOOL","vBool":true},{"key":"http.url","vStr":"http://localhost:9001/rdb/search"},{"key":"http.method","vStr":"GET"},{"key":"http.url.pattern","vStr":"/rdb/search"},{"key":"internal.span.format","vStr":"proto"}],"logs":[{"timestamp":"2024-09-05T07:40:23.975Z","fields":[{"key":"handler.class_simple_name","vStr":"MiddlewareController"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#searchRdb()"},{"key":"event","vStr":"preHandle"},{"key":"handler.method_name","vStr":"searchRdb"}]},{"timestamp":"2024-09-05T07:40:23.982Z","fields":[{"key":"thread","vStr":"http-nio-9001-exec-8"},{"key":"message","vStr":"Span reported: 1b94d80d7aa96789:107d585c969f79bd:1b94d80d7aa96789:1 - Execute"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]},{"timestamp":"2024-09-05T07:40:23.987Z","fields":[{"key":"event","vStr":"afterCompletion"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#searchRdb()"}]}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.18"}]}}
//     */
//    private String callerSpan1 = "{\n" +
//            "    \"traceId\": \"AAAAAAAAAAAblNgNeqlniQ==\",\n" +
//            "    \"spanId\": \"G5TYDXqpZ4k=\",\n" +
//            "    \"operationName\": \"searchRdb\",\n" +
//            "    \"flags\": 1,\n" +
//            "    \"startTime\": \"2024-09-05T07:40:23.973Z\",\n" +
//            "    \"duration\": \"0.014859s\",\n" +
//            "    \"tags\": [\n" +
//            "        {\n" +
//            "            \"key\": \"http.status_code\",\n" +
//            "            \"vType\": \"INT64\",\n" +
//            "            \"vInt64\": \"200\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"component\",\n" +
//            "            \"vStr\": \"java-web-servlet\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"span.kind\",\n" +
//            "            \"vStr\": \"server\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"sampler.type\",\n" +
//            "            \"vStr\": \"const\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"sampler.param\",\n" +
//            "            \"vType\": \"BOOL\",\n" +
//            "            \"vBool\": true\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"http.url\",\n" +
//            "            \"vStr\": \"http://localhost:9001/rdb/search\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"http.method\",\n" +
//            "            \"vStr\": \"GET\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"http.url.pattern\",\n" +
//            "            \"vStr\": \"/rdb/search\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"internal.span.format\",\n" +
//            "            \"vStr\": \"proto\"\n" +
//            "        }\n" +
//            "    ],\n" +
//            "    \"logs\": [\n" +
//            "        {\n" +
//            "            \"timestamp\": \"2024-09-05T07:40:23.975Z\",\n" +
//            "            \"fields\": [\n" +
//            "                {\n" +
//            "                    \"key\": \"handler.class_simple_name\",\n" +
//            "                    \"vStr\": \"MiddlewareController\"\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"key\": \"handler\",\n" +
//            "                    \"vStr\": \"com.sample.provider.api.MiddlewareController#searchRdb()\"\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"key\": \"event\",\n" +
//            "                    \"vStr\": \"preHandle\"\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"key\": \"handler.method_name\",\n" +
//            "                    \"vStr\": \"searchRdb\"\n" +
//            "                }\n" +
//            "            ]\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"timestamp\": \"2024-09-05T07:40:23.987Z\",\n" +
//            "            \"fields\": [\n" +
//            "                {\n" +
//            "                    \"key\": \"event\",\n" +
//            "                    \"vStr\": \"afterCompletion\"\n" +
//            "                },\n" +
//            "                {\n" +
//            "                    \"key\": \"handler\",\n" +
//            "                    \"vStr\": \"com.sample.provider.api.MiddlewareController#searchRdb()\"\n" +
//            "                }\n" +
//            "            ]\n" +
//            "        }\n" +
//            "    ],\n" +
//            "    \"process\": {\n" +
//            "        \"serviceName\": \"hello-service\",\n" +
//            "        \"tags\": [\n" +
//            "            {\n" +
//            "                \"key\": \"hostname\",\n" +
//            "                \"vStr\": \"sample-provider\"\n" +
//            "            },\n" +
//            "            {\n" +
//            "                \"key\": \"jaeger.version\",\n" +
//            "                \"vStr\": \"Java-1.3.2\"\n" +
//            "            },\n" +
//            "            {\n" +
//            "                \"key\": \"ip\",\n" +
//            "                \"vStr\": \"172.17.0.18\"\n" +
//            "            }\n" +
//            "        ]\n" +
//            "    }\n" +
//            "}";
//
//
//    /**
//     *
//     * {"traceId":"AAAAAAAAAAAblNgNeqlniQ==","spanId":"EH1YXJafeb0=","operationName":"Execute","references":[{"traceId":"AAAAAAAAAAAblNgNeqlniQ==","spanId":"G5TYDXqpZ4k="}],"flags":1,"startTime":"2024-09-05T07:40:23.981Z","duration":"0.000406s","tags":[{"key":"db.instance","vStr":"test"},{"key":"component","vStr":"java-jdbc"},{"key":"db.type","vStr":"h2"},{"key":"span.kind","vStr":"client"},{"key":"db.statement","vStr":"select\n         \n        id, nickname, mobile, password, role, create_time, update_time\n     \n        from user\n        where id = ?"},{"key":"peer.service","vStr":"test[h2(localhost:-1)]"},{"key":"peer.address","vStr":"localhost:-1"},{"key":"internal.span.format","vStr":"proto"}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.18"}]}}
//     */
//    private String calleeSpan1 = "{\n" +
//            "    \"traceId\": \"AAAAAAAAAAAblNgNeqlniQ==\",\n" +
//            "    \"spanId\": \"EH1YXJafeb0=\",\n" +
//            "    \"operationName\": \"Execute\",\n" +
//            "    \"references\": [\n" +
//            "        {\n" +
//            "            \"traceId\": \"AAAAAAAAAAAblNgNeqlniQ==\",\n" +
//            "            \"spanId\": \"G5TYDXqpZ4k=\"\n" +
//            "        }\n" +
//            "    ],\n" +
//            "    \"flags\": 1,\n" +
//            "    \"startTime\": \"2024-09-05T07:40:23.981Z\",\n" +
//            "    \"duration\": \"0.000406s\",\n" +
//            "    \"tags\": [\n" +
//            "        {\n" +
//            "            \"key\": \"db.instance\",\n" +
//            "            \"vStr\": \"test\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"component\",\n" +
//            "            \"vStr\": \"java-jdbc\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"db.type\",\n" +
//            "            \"vStr\": \"h2\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"span.kind\",\n" +
//            "            \"vStr\": \"client\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"db.statement\",\n" +
//            "            \"vStr\": \"select\\n         \\n        id, nickname, mobile, password, role, create_time, update_time\\n     \\n        from user\\n        where id = ?\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"peer.service\",\n" +
//            "            \"vStr\": \"test[h2(localhost:-1)]\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"peer.address\",\n" +
//            "            \"vStr\": \"localhost:-1\"\n" +
//            "        },\n" +
//            "        {\n" +
//            "            \"key\": \"internal.span.format\",\n" +
//            "            \"vStr\": \"proto\"\n" +
//            "        }\n" +
//            "    ],\n" +
//            "    \"process\": {\n" +
//            "        \"serviceName\": \"hello-service\",\n" +
//            "        \"tags\": [\n" +
//            "            {\n" +
//            "                \"key\": \"hostname\",\n" +
//            "                \"vStr\": \"sample-provider\"\n" +
//            "            },\n" +
//            "            {\n" +
//            "                \"key\": \"jaeger.version\",\n" +
//            "                \"vStr\": \"Java-1.3.2\"\n" +
//            "            },\n" +
//            "            {\n" +
//            "                \"key\": \"ip\",\n" +
//            "                \"vStr\": \"172.17.0.18\"\n" +
//            "            }\n" +
//            "        ]\n" +
//            "    }\n" +
//            "}\n";
//
//    /**
//     *
//     {"traceId":"AAAAAAAAAACt97dnG5VE1w==","spanId":"rfe3ZxuVRNc=","operationName":"setKeyToRedis","flags":1,"startTime":"2024-09-06T09:00:10.454Z","duration":"0.006649s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"component","vStr":"java-web-servlet"},{"key":"span.kind","vStr":"server"},{"key":"sampler.type","vStr":"const"},{"key":"sampler.param","vType":"BOOL","vBool":true},{"key":"http.url","vStr":"http://localhost:9001/redis/set"},{"key":"http.method","vStr":"GET"},{"key":"http.url.pattern","vStr":"/redis/set"},{"key":"internal.span.format","vStr":"proto"}],"logs":[{"timestamp":"2024-09-06T09:00:10.454Z","fields":[{"key":"handler.class_simple_name","vStr":"MiddlewareController"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#setKeyToRedis()"},{"key":"event","vStr":"preHandle"},{"key":"handler.method_name","vStr":"setKeyToRedis"}]},{"timestamp":"2024-09-06T09:00:10.459Z","fields":[{"key":"thread","vStr":"http-nio-9001-exec-4"},{"key":"message","vStr":"Span reported: adf7b7671b9544d7:8b79911e2cefa0e5:adf7b7671b9544d7:1 - SET"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]},{"timestamp":"2024-09-06T09:00:10.460Z","fields":[{"key":"event","vStr":"afterCompletion"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#setKeyToRedis()"}]}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.3"}]}}
//
//     */
//    private String callerSpan2 = "{\"traceId\":\"AAAAAAAAAACt97dnG5VE1w==\",\"spanId\":\"rfe3ZxuVRNc=\",\"operationName\":\"setKeyToRedis\",\"flags\":1,\"startTime\":\"2024-09-06T09:00:10.454Z\",\"duration\":\"0.006649s\",\"tags\":[{\"key\":\"http.status_code\",\"vType\":\"INT64\",\"vInt64\":\"200\"},{\"key\":\"component\",\"vStr\":\"java-web-servlet\"},{\"key\":\"span.kind\",\"vStr\":\"server\"},{\"key\":\"sampler.type\",\"vStr\":\"const\"},{\"key\":\"sampler.param\",\"vType\":\"BOOL\",\"vBool\":true},{\"key\":\"http.url\",\"vStr\":\"http://localhost:9001/redis/set\"},{\"key\":\"http.method\",\"vStr\":\"GET\"},{\"key\":\"http.url.pattern\",\"vStr\":\"/redis/set\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"logs\":[{\"timestamp\":\"2024-09-06T09:00:10.454Z\",\"fields\":[{\"key\":\"handler.class_simple_name\",\"vStr\":\"MiddlewareController\"},{\"key\":\"handler\",\"vStr\":\"com.sample.provider.api.MiddlewareController#setKeyToRedis()\"},{\"key\":\"event\",\"vStr\":\"preHandle\"},{\"key\":\"handler.method_name\",\"vStr\":\"setKeyToRedis\"}]},{\"timestamp\":\"2024-09-06T09:00:10.459Z\",\"fields\":[{\"key\":\"thread\",\"vStr\":\"http-nio-9001-exec-4\"},{\"key\":\"message\",\"vStr\":\"Span reported: adf7b7671b9544d7:8b79911e2cefa0e5:adf7b7671b9544d7:1 - SET\"},{\"key\":\"level\",\"vStr\":\"INFO\"},{\"key\":\"logger\",\"vStr\":\"io.jaegertracing.internal.reporters.LoggingReporter\"}]},{\"timestamp\":\"2024-09-06T09:00:10.460Z\",\"fields\":[{\"key\":\"event\",\"vStr\":\"afterCompletion\"},{\"key\":\"handler\",\"vStr\":\"com.sample.provider.api.MiddlewareController#setKeyToRedis()\"}]}],\"process\":{\"serviceName\":\"hello-service\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-provider\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.3\"}]}}\n";
//
//
//    /**
//     {"traceId":"AAAAAAAAAACt97dnG5VE1w==","spanId":"i3mRHizvoOU=","operationName":"SET","references":[{"traceId":"AAAAAAAAAACt97dnG5VE1w==","spanId":"rfe3ZxuVRNc="}],"flags":1,"startTime":"2024-09-06T09:00:10.456Z","duration":"0.003599s","tags":[{"key":"component","vStr":"java-redis"},{"key":"db.type","vStr":"redis"},{"key":"span.kind","vStr":"client"},{"key":"key","vStr":"test-key"},{"key":"internal.span.format","vStr":"proto"}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.3"}]}}
//
//     */
//    private String calleeSpan2 = "{\"traceId\":\"AAAAAAAAAACt97dnG5VE1w==\",\"spanId\":\"i3mRHizvoOU=\",\"operationName\":\"SET\",\"references\":[{\"traceId\":\"AAAAAAAAAACt97dnG5VE1w==\",\"spanId\":\"rfe3ZxuVRNc=\"}],\"flags\":1,\"startTime\":\"2024-09-06T09:00:10.456Z\",\"duration\":\"0.003599s\",\"tags\":[{\"key\":\"component\",\"vStr\":\"java-redis\"},{\"key\":\"db.type\",\"vStr\":\"redis\"},{\"key\":\"span.kind\",\"vStr\":\"client\"},{\"key\":\"key\",\"vStr\":\"test-key\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"process\":{\"serviceName\":\"hello-service\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-provider\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.3\"}]}}\n";
//
//
//    /**
//     {"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"J+7ex6K7ktI=","operationName":"saveRdb","flags":1,"startTime":"2024-09-09T08:42:45.139Z","duration":"0.265790s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"component","vStr":"java-web-servlet"},{"key":"span.kind","vStr":"server"},{"key":"sampler.type","vStr":"const"},{"key":"sampler.param","vType":"BOOL","vBool":true},{"key":"http.url","vStr":"http://localhost:9002/rdb/save"},{"key":"http.method","vStr":"GET"},{"key":"http.url.pattern","vStr":"/rdb/save"},{"key":"internal.span.format","vStr":"proto"}],"logs":[{"timestamp":"2024-09-09T08:42:45.160Z","fields":[{"key":"handler.class_simple_name","vStr":"MiddlewareController"},{"key":"handler","vStr":"com.sample.consumer.controller.MiddlewareController#saveRdb()"},{"key":"event","vStr":"preHandle"},{"key":"handler.method_name","vStr":"saveRdb"}]},{"timestamp":"2024-09-09T08:42:45.368Z","fields":[{"key":"thread","vStr":"http-nio-9002-exec-1"},{"key":"message","vStr":"Span reported: 27eedec7a2bb92d2:92d0ff600929bb1d:27eedec7a2bb92d2:1 - GET"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]},{"timestamp":"2024-09-09T08:42:45.400Z","fields":[{"key":"event","vStr":"afterCompletion"},{"key":"handler","vStr":"com.sample.consumer.controller.MiddlewareController#saveRdb()"}]}],"process":{"serviceName":"feign-consumer","tags":[{"key":"hostname","vStr":"sample-consumer"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.19"}]}}
//
//     */
//    private String multiService1CallerServiceServerSpan = "{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"J+7ex6K7ktI=\",\"operationName\":\"saveRdb\",\"flags\":1,\"startTime\":\"2024-09-09T08:42:45.139Z\",\"duration\":\"0.265790s\",\"tags\":[{\"key\":\"http.status_code\",\"vType\":\"INT64\",\"vInt64\":\"200\"},{\"key\":\"component\",\"vStr\":\"java-web-servlet\"},{\"key\":\"span.kind\",\"vStr\":\"server\"},{\"key\":\"sampler.type\",\"vStr\":\"const\"},{\"key\":\"sampler.param\",\"vType\":\"BOOL\",\"vBool\":true},{\"key\":\"http.url\",\"vStr\":\"http://localhost:9002/rdb/save\"},{\"key\":\"http.method\",\"vStr\":\"GET\"},{\"key\":\"http.url.pattern\",\"vStr\":\"/rdb/save\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"logs\":[{\"timestamp\":\"2024-09-09T08:42:45.160Z\",\"fields\":[{\"key\":\"handler.class_simple_name\",\"vStr\":\"MiddlewareController\"},{\"key\":\"handler\",\"vStr\":\"com.sample.consumer.controller.MiddlewareController#saveRdb()\"},{\"key\":\"event\",\"vStr\":\"preHandle\"},{\"key\":\"handler.method_name\",\"vStr\":\"saveRdb\"}]},{\"timestamp\":\"2024-09-09T08:42:45.368Z\",\"fields\":[{\"key\":\"thread\",\"vStr\":\"http-nio-9002-exec-1\"},{\"key\":\"message\",\"vStr\":\"Span reported: 27eedec7a2bb92d2:92d0ff600929bb1d:27eedec7a2bb92d2:1 - GET\"},{\"key\":\"level\",\"vStr\":\"INFO\"},{\"key\":\"logger\",\"vStr\":\"io.jaegertracing.internal.reporters.LoggingReporter\"}]},{\"timestamp\":\"2024-09-09T08:42:45.400Z\",\"fields\":[{\"key\":\"event\",\"vStr\":\"afterCompletion\"},{\"key\":\"handler\",\"vStr\":\"com.sample.consumer.controller.MiddlewareController#saveRdb()\"}]}],\"process\":{\"serviceName\":\"feign-consumer\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-consumer\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.19\"}]}}\n";
//
//
//    /**
//     {"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"ktD/YAkpux0=","operationName":"GET","references":[{"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"J+7ex6K7ktI="}],"flags":1,"startTime":"2024-09-09T08:42:45.175Z","duration":"0.192982s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"http.url","vStr":"http://hello-service/middleware?middlewareName=rdb\u0026operation=save"},{"key":"component","vStr":"feign"},{"key":"span.kind","vStr":"client"},{"key":"http.method","vStr":"GET"},{"key":"internal.span.format","vStr":"proto"}],"logs":[{"timestamp":"2024-09-09T08:42:45.367Z","fields":[{"key":"thread","vStr":"http-nio-9002-exec-1"},{"key":"message","vStr":"Span reported: 27eedec7a2bb92d2:383e908ca1a2c19f:92d0ff600929bb1d:1 - GET"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]}],"process":{"serviceName":"feign-consumer","tags":[{"key":"hostname","vStr":"sample-consumer"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.19"}]}}
//     */
//
//    private String multiService1CallerServiceClient1Span = "{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"ktD/YAkpux0=\",\"operationName\":\"GET\",\"references\":[{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"J+7ex6K7ktI=\"}],\"flags\":1,\"startTime\":\"2024-09-09T08:42:45.175Z\",\"duration\":\"0.192982s\",\"tags\":[{\"key\":\"http.status_code\",\"vType\":\"INT64\",\"vInt64\":\"200\"},{\"key\":\"http.url\",\"vStr\":\"http://hello-service/middleware?middlewareName=rdb\\u0026operation=save\"},{\"key\":\"component\",\"vStr\":\"feign\"},{\"key\":\"span.kind\",\"vStr\":\"client\"},{\"key\":\"http.method\",\"vStr\":\"GET\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"logs\":[{\"timestamp\":\"2024-09-09T08:42:45.367Z\",\"fields\":[{\"key\":\"thread\",\"vStr\":\"http-nio-9002-exec-1\"},{\"key\":\"message\",\"vStr\":\"Span reported: 27eedec7a2bb92d2:383e908ca1a2c19f:92d0ff600929bb1d:1 - GET\"},{\"key\":\"level\",\"vStr\":\"INFO\"},{\"key\":\"logger\",\"vStr\":\"io.jaegertracing.internal.reporters.LoggingReporter\"}]}],\"process\":{\"serviceName\":\"feign-consumer\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-consumer\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.19\"}]}}\n ";
//
//
//    /**
//     {"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"OD6QjKGiwZ8=","operationName":"GET","references":[{"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"ktD/YAkpux0="}],"flags":1,"startTime":"2024-09-09T08:42:45.176Z","duration":"0.191019s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"http.url","vStr":"http://hello-service/middleware?middlewareName=rdb\u0026operation=save"},{"key":"component","vStr":"feign"},{"key":"span.kind","vStr":"client"},{"key":"http.method","vStr":"GET"},{"key":"internal.span.format","vStr":"proto"}],"process":{"serviceName":"feign-consumer","tags":[{"key":"hostname","vStr":"sample-consumer"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.19"}]}}
//
//     */
//    private String multiService1CallerServiceClient2Span = "{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"OD6QjKGiwZ8=\",\"operationName\":\"GET\",\"references\":[{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"ktD/YAkpux0=\"}],\"flags\":1,\"startTime\":\"2024-09-09T08:42:45.176Z\",\"duration\":\"0.191019s\",\"tags\":[{\"key\":\"http.status_code\",\"vType\":\"INT64\",\"vInt64\":\"200\"},{\"key\":\"http.url\",\"vStr\":\"http://hello-service/middleware?middlewareName=rdb\\u0026operation=save\"},{\"key\":\"component\",\"vStr\":\"feign\"},{\"key\":\"span.kind\",\"vStr\":\"client\"},{\"key\":\"http.method\",\"vStr\":\"GET\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"process\":{\"serviceName\":\"feign-consumer\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-consumer\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.19\"}]}}\n";
//
//
//    /**
//     * {"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"+VylZSo6ACg=","operationName":"callMiddleware","references":[{"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"OD6QjKGiwZ8="}],"flags":1,"startTime":"2024-09-09T08:42:45.286Z","duration":"0.082229s","tags":[{"key":"http.status_code","vType":"INT64","vInt64":"200"},{"key":"component","vStr":"java-web-servlet"},{"key":"span.kind","vStr":"server"},{"key":"http.url","vStr":"http://172.17.0.18:9001/middleware"},{"key":"http.method","vStr":"GET"},{"key":"http.url.pattern","vStr":"/middleware"},{"key":"internal.span.format","vStr":"proto"}],"logs":[{"timestamp":"2024-09-09T08:42:45.287Z","fields":[{"key":"handler.class_simple_name","vStr":"MiddlewareController"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#callMiddleware(String, String)"},{"key":"event","vStr":"preHandle"},{"key":"handler.method_name","vStr":"callMiddleware"}]},{"timestamp":"2024-09-09T08:42:45.317Z","fields":[{"key":"thread","vStr":"http-nio-9001-exec-5"},{"key":"message","vStr":"Span reported: 27eedec7a2bb92d2:21d9aeba9efa64c6:f95ca5652a3a0028:1 - update"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]},{"timestamp":"2024-09-09T08:42:45.358Z","fields":[{"key":"thread","vStr":"http-nio-9001-exec-5"},{"key":"message","vStr":"Span reported: 27eedec7a2bb92d2:74aac17a0841b3ad:f95ca5652a3a0028:1 - Execute"},{"key":"level","vStr":"INFO"},{"key":"logger","vStr":"io.jaegertracing.internal.reporters.LoggingReporter"}]},{"timestamp":"2024-09-09T08:42:45.368Z","fields":[{"key":"event","vStr":"afterCompletion"},{"key":"handler","vStr":"com.sample.provider.api.MiddlewareController#callMiddleware(String, String)"}]}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.18"}]}}
//     */
//    private String multiService1CalleeServiceServerSpan = "{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"+VylZSo6ACg=\",\"operationName\":\"callMiddleware\",\"references\":[{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"OD6QjKGiwZ8=\"}],\"flags\":1,\"startTime\":\"2024-09-09T08:42:45.286Z\",\"duration\":\"0.082229s\",\"tags\":[{\"key\":\"http.status_code\",\"vType\":\"INT64\",\"vInt64\":\"200\"},{\"key\":\"component\",\"vStr\":\"java-web-servlet\"},{\"key\":\"span.kind\",\"vStr\":\"server\"},{\"key\":\"http.url\",\"vStr\":\"http://172.17.0.18:9001/middleware\"},{\"key\":\"http.method\",\"vStr\":\"GET\"},{\"key\":\"http.url.pattern\",\"vStr\":\"/middleware\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"logs\":[{\"timestamp\":\"2024-09-09T08:42:45.287Z\",\"fields\":[{\"key\":\"handler.class_simple_name\",\"vStr\":\"MiddlewareController\"},{\"key\":\"handler\",\"vStr\":\"com.sample.provider.api.MiddlewareController#callMiddleware(String, String)\"},{\"key\":\"event\",\"vStr\":\"preHandle\"},{\"key\":\"handler.method_name\",\"vStr\":\"callMiddleware\"}]},{\"timestamp\":\"2024-09-09T08:42:45.317Z\",\"fields\":[{\"key\":\"thread\",\"vStr\":\"http-nio-9001-exec-5\"},{\"key\":\"message\",\"vStr\":\"Span reported: 27eedec7a2bb92d2:21d9aeba9efa64c6:f95ca5652a3a0028:1 - update\"},{\"key\":\"level\",\"vStr\":\"INFO\"},{\"key\":\"logger\",\"vStr\":\"io.jaegertracing.internal.reporters.LoggingReporter\"}]},{\"timestamp\":\"2024-09-09T08:42:45.358Z\",\"fields\":[{\"key\":\"thread\",\"vStr\":\"http-nio-9001-exec-5\"},{\"key\":\"message\",\"vStr\":\"Span reported: 27eedec7a2bb92d2:74aac17a0841b3ad:f95ca5652a3a0028:1 - Execute\"},{\"key\":\"level\",\"vStr\":\"INFO\"},{\"key\":\"logger\",\"vStr\":\"io.jaegertracing.internal.reporters.LoggingReporter\"}]},{\"timestamp\":\"2024-09-09T08:42:45.368Z\",\"fields\":[{\"key\":\"event\",\"vStr\":\"afterCompletion\"},{\"key\":\"handler\",\"vStr\":\"com.sample.provider.api.MiddlewareController#callMiddleware(String, String)\"}]}],\"process\":{\"serviceName\":\"hello-service\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-provider\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.18\"}]}}\n";
//
//
//    /**
//     {"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"dKrBeghBs60=","operationName":"Execute","references":[{"traceId":"AAAAAAAAAAAn7t7HoruS0g==","spanId":"+VylZSo6ACg="}],"flags":1,"startTime":"2024-09-09T08:42:45.352Z","duration":"0.006030s","tags":[{"key":"db.instance","vStr":"test"},{"key":"component","vStr":"java-jdbc"},{"key":"db.type","vStr":"h2"},{"key":"span.kind","vStr":"client"},{"key":"db.statement","vStr":"insert into user ( \n        id, nickname, mobile, password, role, create_time, update_time\n     )\n        VALUES (?, ?, ?, ?, ?, ?, ?)"},{"key":"peer.service","vStr":"test[h2(localhost:-1)]"},{"key":"peer.address","vStr":"localhost:-1"},{"key":"internal.span.format","vStr":"proto"}],"process":{"serviceName":"hello-service","tags":[{"key":"hostname","vStr":"sample-provider"},{"key":"jaeger.version","vStr":"Java-1.3.2"},{"key":"ip","vStr":"172.17.0.18"}]}}
//j
//     */
//    private String multiService1CalleeServiceClientSpan = "{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"dKrBeghBs60=\",\"operationName\":\"Execute\",\"references\":[{\"traceId\":\"AAAAAAAAAAAn7t7HoruS0g==\",\"spanId\":\"+VylZSo6ACg=\"}],\"flags\":1,\"startTime\":\"2024-09-09T08:42:45.352Z\",\"duration\":\"0.006030s\",\"tags\":[{\"key\":\"db.instance\",\"vStr\":\"test\"},{\"key\":\"component\",\"vStr\":\"java-jdbc\"},{\"key\":\"db.type\",\"vStr\":\"h2\"},{\"key\":\"span.kind\",\"vStr\":\"client\"},{\"key\":\"db.statement\",\"vStr\":\"insert into user ( \\n        id, nickname, mobile, password, role, create_time, update_time\\n     )\\n        VALUES (?, ?, ?, ?, ?, ?, ?)\"},{\"key\":\"peer.service\",\"vStr\":\"test[h2(localhost:-1)]\"},{\"key\":\"peer.address\",\"vStr\":\"localhost:-1\"},{\"key\":\"internal.span.format\",\"vStr\":\"proto\"}],\"process\":{\"serviceName\":\"hello-service\",\"tags\":[{\"key\":\"hostname\",\"vStr\":\"sample-provider\"},{\"key\":\"jaeger.version\",\"vStr\":\"Java-1.3.2\"},{\"key\":\"ip\",\"vStr\":\"172.17.0.18\"}]}}\n";
//
}
