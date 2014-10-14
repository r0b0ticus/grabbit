package com.twc.webcms.sync.client.services.impl

import com.twc.webcms.sync.client.batch.ClientBatchJob
import com.twc.webcms.sync.client.services.ClientService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property as ScrProperty
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.sling.jcr.api.SlingRepository
import org.osgi.service.component.ComponentContext
import org.springframework.context.ConfigurableApplicationContext

@Slf4j
@CompileStatic
@Component(label = "Content Sync Client Service", description = "Content Sync Client Service", immediate = true, metatype = true, enabled = true)
@Service(ClientService)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultClientService implements ClientService {

    public static final int BATCH_SIZE = 1000

    @ScrProperty(label = "Sync Server Hostname", description = "Sync Server Hostname")
    public static final String SYNC_SERVER_HOSTNAME = "sync.server.hostname"
    private String syncServerHostname

    @ScrProperty(label = "Sync Server Port", description = "Sync Server Port")
    public static final String SYNC_SERVER_PORT = "sync.server.port"
    private String syncServerPort

    @ScrProperty(label = "Sync Server Username", description = "Sync Server Username")
    public static final String SYNC_SERVER_USERNAME = "sync.server.username"
    private String syncServerUsername

    @ScrProperty(label = "Sync Server Password", description = "Sync Server Password")
    public static final String SYNC_SERVER_PASSWORD = "sync.server.password"
    private String syncServerPassword

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    @Activate
    void activate(ComponentContext componentContext) {
        log.info "Activate\n\n"
        syncServerHostname = componentContext.properties[SYNC_SERVER_HOSTNAME] as String
        syncServerPort = componentContext.properties[SYNC_SERVER_PORT] as String
        syncServerUsername = componentContext.properties[SYNC_SERVER_USERNAME] as String
        syncServerPassword = componentContext.properties[SYNC_SERVER_PASSWORD] as String
    }

    @Override
    Collection<Long> initiateGrab(Collection<String> whiteList) {

        Collection<Long> jobExecutionIds = []
        for(String path: whiteList) {
            final Long currentJobExecutionId = initiate(path)
            if(currentJobExecutionId == -1) throw new IllegalStateException("Failed to initiate job for path: ${path}")
            jobExecutionIds << currentJobExecutionId
        }
        return jobExecutionIds

    }

    private Long initiate(String path) {
        try {
            ClientBatchJob batchJob = configuredClientBatchJob(syncServerHostname, syncServerPort, syncServerUsername, syncServerPassword, path)
            Long id = batchJob.start()
            return id
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for current Path: ${[path]}", e
            return -1
        }

    }

    private ClientBatchJob configuredClientBatchJob(String host, String port, String username, String password, String path) {
        ClientBatchJob batchJob = new ClientBatchJob.ServerBuilder(configurableApplicationContext)
                                    .andServer(host, port)
                                    .andCredentials(username, password)
                                    .andPath(path)
                                    .build()
        batchJob
    }

}
