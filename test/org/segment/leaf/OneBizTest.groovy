package org.segment.leaf

import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Before
import spock.lang.Specification

import javax.sql.DataSource
import java.util.concurrent.CountDownLatch

@Slf4j
class OneBizTest extends Specification {
    private Sql sql

    @Before
    void before() {
        DataSource dataSource = new JdbcDataSource()
        dataSource.url = 'jdbc:h2:~/test-segment-leaf'
        dataSource.user = 'sa'
        dataSource.password = ''

        sql = new Sql(dataSource)

        String ddl = '''
CREATE TABLE if not exists leaf_alloc (
  biz_tag varchar(128)  NOT NULL DEFAULT '',
  max_id bigint(20) NOT NULL DEFAULT '1',
  step int(11) NOT NULL,
  description varchar(256)  DEFAULT NULL,
  update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (biz_tag)
)
'''
        sql.execute(ddl)
        clearTable()
        DsHolder.instance.dataSource = dataSource
    }

    @After
    void clearTable() {
        sql.executeUpdate('delete from leaf_alloc')
        log.info 'clear table leaf_alloc'
    }

    void addOneRow() {
        String addSql = '''
insert into leaf_alloc(biz_tag, max_id, step, description) values('leaf-segment-test', 1, 2000, 'Test leaf Segment Mode Get Id')
'''
        sql.executeUpdate(addSql)
    }

    void testGetAllList() {
        given:
        addOneRow()
        and:
        def r = OneBiz.getAllList()
        expect:
        r.size() == 1
        r[0].bizTag == 'leaf-segment-test'
    }

    void testUpdateMaxId() {
        given:
        addOneRow()
        and:
        def one = OneBiz.updateMaxId('leaf-segment-test')
        expect:
        one.maxId == 2001
    }

    void testUpdateMaxIdMultiThread() {
        given:
        addOneRow()
        and:
        final int loopTimes = 100
        def set = Collections.synchronizedSortedSet(new TreeSet<Long>())
        def latch = new CountDownLatch(loopTimes)
        loopTimes.times {
            Thread.start {
                try {
                    set << OneBiz.updateMaxId('leaf-segment-test').maxId
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        log.info 'first max id: ' + set[0]
        log.info 'middle max id: ' + set[(loopTimes / 2) as int]
        log.info 'last max id: ' + set[-1]
        expect:
        set.size() == 100
    }

    void testUpdateMaxIdByCustomStep() {
        given:
        addOneRow()
        and:
        def one = OneBiz.updateMaxIdByCustomStep('leaf-segment-test', 10000)
        expect:
        one.maxId == 10001
    }

    void testGetAllBizTagList() {
        given:
        addOneRow()
        and:
        def list = OneBiz.getAllBizTagList()
        expect:
        list[0] == 'leaf-segment-test'
    }
}
