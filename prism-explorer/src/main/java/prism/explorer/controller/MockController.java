package prism.explorer.controller;

import prism.explorer.dao.PrismExplorerDatabaseDao;
import prism.explorer.service.MockDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/prism-explorer/api/mock")
public class MockController {

    private static final Logger log = LoggerFactory.getLogger(MockController.class);

    private final PrismExplorerDatabaseDao explorerDao;
    private final MockDataService mockDataService;

    private final ConcurrentHashMap<String, MockDataService.TaskState> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MockController(PrismExplorerDatabaseDao explorerDao,
                          MockDataService mockDataService) {
        this.explorerDao = explorerDao;
        this.mockDataService = mockDataService;
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> data(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        if (!explorerDao.isInitDone()) {
            return ResponseEntity.ok(Map.of(
                    "done", true,
                    "message", "Please initialize the database before generating mock data"
            ));
        }

        log.info("Received data generation request: startDate={}, endDate={}", startDate, endDate);
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockDataService.TaskState state = new MockDataService.TaskState(taskId);
        state.update("Preparing...", false);
        tasks.put(taskId, state);

        executor.submit(() -> mockDataService.executeData(startDate, endDate, state));

        return ResponseEntity.accepted().body(state.snapshot());
    }

    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clear() {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        MockDataService.TaskState state = new MockDataService.TaskState(taskId);
        state.update("Starting cleanup...", false);
        tasks.put(taskId, state);

        executor.submit(() -> mockDataService.executeClear(state));

        return ResponseEntity.accepted().body(state.snapshot());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of("initDone", explorerDao.isInitDone()));
    }

    @GetMapping("/data-status")
    public ResponseEntity<Map<String, Object>> getDataStatus() {
        return ResponseEntity.ok(Map.of("generated", explorerDao.isMockDataGenerated()));
    }

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress(@RequestParam String taskId) {
        log.debug("Querying progress: taskId={}", taskId);
        MockDataService.TaskState state = tasks.get(taskId);
        if (state == null) {
            return ResponseEntity.ok(Map.of("done", false, "message", "Task does not exist or has expired"));
        }
        return ResponseEntity.ok(state.snapshot());
    }
}
