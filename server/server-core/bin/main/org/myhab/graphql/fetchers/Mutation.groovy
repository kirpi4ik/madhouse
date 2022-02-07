package org.myhab.graphql.fetchers


import grails.events.EventPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j
import org.myhab.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@org.springframework.transaction.annotation.Transactional
class Mutation implements EventPublisher {

    @Autowired
    UserService userService

    public DataFetcher pushEvent() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def pushedEvent = environment.getArgument("input")
                publish("${pushedEvent['p0']}", pushedEvent)
                return pushedEvent
            }
        }
    }

    public DataFetcher userRolesSave() {
        return userService
    }
}
