package sync.voxel.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VoxelApp {
	public static final String BASE_URL = "/api/";
	public static final String STATISTICS_URL = "/api/stats/";
	public static final String PACK_UPLOAD_URL = "/api/host/pack/";
	public static final Logger LOGGER = LoggerFactory.getLogger("API");

	public static void main(String[] args) {
		SpringApplication.run(VoxelApp.class, args);
	}

}
