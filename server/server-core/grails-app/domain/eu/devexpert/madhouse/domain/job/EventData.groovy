package eu.devexpert.madhouse.domain.job

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Event
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class EventData extends Event  {
    String category // event category - optional

//
//  def beforeInsert() {
//    super.beforeInsert()
//    entityType = EntityType.EVENT_DATA
//  }
    static mapping = {
        table '`event_log`'
        version false
    }
    static constraints = {
        category nullable: true
        p4 nullable: true
        p5 nullable: true
        p6 nullable: true
    }
    static graphql = GraphQLMapping.lazy {
        query('eventsByP2', [EventData]) {
            argument('p2', String)
            argument('count', Integer)
            argument('offset', Integer)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    EventData.createCriteria().list(max: environment.getArgument('count'), offset: environment.getArgument('offset')) {
                        eq("p2", environment.getArgument('p2'))
                        order("tsCreated", "desc")
                    }
                }
            })
        }
    }
}
