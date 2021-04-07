package is.ispan.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import is.ispan.scheduler.Scheduler;

@RestController
@RequestMapping("/api/vi/generate-ascii")
public class GenerateAsciiController {
	
	@Autowired
	private Scheduler scheduler;
	
	@GetMapping
	public ResponseEntity<String> processFiles() throws IOException {
	
		return ResponseEntity.ok().body(scheduler.generateASCIIFiles());
	}
}
