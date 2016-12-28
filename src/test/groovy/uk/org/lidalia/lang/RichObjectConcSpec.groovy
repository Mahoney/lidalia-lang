package uk.org.lidalia.lang

import spock.lang.Specification
import uk.org.lidalia.lang.testclasses.ClassA

import java.util.concurrent.CountDownLatch

class RichObjectConcSpec extends Specification {

    def 'behaviour with concurrent threads'() {

        given:
            def procs = Runtime.runtime.availableProcessors()
            def start = new CountDownLatch(1)
            def ready = new CountDownLatch(procs)
            def o1 = new ClassA(value1: 'blah')
            def o2 = new ClassA(value1: 'blah')

        when:
            def results = [].asSynchronized()
            def threads = (0..procs).collect {
                Thread.start {
                    ready.countDown()
                    start.await()
                    results << (o1 == o2)
                }
            }
            ready.await()
            start.countDown()
            threads*.join()

        then:
            results.every { it == true }
    }
}
