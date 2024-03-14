package io.william.debrid

/*@SpringBootTest(
    classes = [DebridApplication::class, IntegrationTestContextConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)*/
/*
class TorrentEmulationTests {

    companion object {
        val port = TestSocketUtils.findAvailableTcpPort()

        private var mockServer: ClientAndServer? = null

        @JvmStatic
        @BeforeAll
        fun startServer() {
            mockServer = ClientAndServer.startClientAndServer(port)

        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            mockServer!!.stop()
            FileUtils.deleteDirectory(File("/tmp/debridavtests/"))
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerPgProperties(registry: DynamicPropertyRegistry) {
            registry.add("premiumize.baseurl") { "http://localhost:$port" }
        }
    }


    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun addingTorrentProducesDebridFileWhenTorrentCached() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet")
        parts.part("category", "test")
        parts.part("paused", "false")

        mockIsCached(port)
        mockCachedContents(port)

        //when
        webTestClient.post()
            .uri("/api/v2/torrents/add")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(parts.build()))
            .exchange()
            .expectStatus().is2xxSuccessful

        //then
        assertTrue(File("/tmp/debridavtests/downloads/a/b/c.debridfile").exists())
    }
}*/
