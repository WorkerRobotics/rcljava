package com.workerrobotics.rcljava.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ros2.rcl.RclLib;

import com.workerrobotics.rcljava.core.Executor;
import com.workerrobotics.rcljava.core.Node;
import com.workerrobotics.rcljava.core.RosContext;
import com.workerrobotics.rcljava.core.RosRuntime;
import com.workerrobotics.rcljava.loader.RosConfig;

class ExecutorTest {

    @BeforeAll
    static void setup() {
        // Zorg dat de native libraries geladen zijn en de context leeft
        RosConfig cfg = RosConfig.builder()
        // Intentionally do not set nativeLibraryDirs to exercise auto-detection.
        .strict(false) // skeleton: don't fail hard if libs not present on the test machine
        .build();

        RosRuntime.initialize(new String[0], cfg); 
    }

    @Test
    void testSpinExitsOnContextShutdown() {
        Executor executor = new Executor();
        try (Node node = new CustomNode("Test_node")) {
            // Start de spin in een aparte thread
            Thread spinThread = Thread.ofVirtual().start(() -> executor.spin(node));
            
            // Geef het even de tijd om te starten
            Thread.sleep(100);
            
            // Trigger de shutdown
            RosContext.shutdown(); 
            
            // Wacht tot de thread stopt. Als spin() goed werkt, 
            // stopt de loop omdat rcl_context_is_valid false wordt.
            spinThread.join(Duration.ofSeconds(2));
            
            if (spinThread.isAlive()) {
                fail("Executor did not stop after context shutdown");
            }
        } catch(InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException.getMessage());
        }
    }

    @Test
    void testSpinExitsAfter10SecondsOnContextShutdown() {
        Executor executor = new Executor();
        try (Node node = new CustomNode("Test_node_10s")) {
            // Start de spin in een aparte thread
            Thread spinThread = Thread.ofVirtual().start(() -> executor.spin(node));
            
            // Geef het even de tijd om te starten
            Thread.sleep(10000);
            
            // Trigger de shutdown
            RosContext.shutdown(); 
            
            // Wacht tot de thread stopt. Als spin() goed werkt, 
            // stopt de loop omdat rcl_context_is_valid false wordt.
            spinThread.join(Duration.ofSeconds(2));
            
            if (spinThread.isAlive()) {
                fail("Executor did not stop after context shutdown");
            }
        } catch(InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException.getMessage());
        }
    }

    @Test
    void testPubSub() {
        Executor executor = new Executor();
        
        // Gebruik try-with-resources voor beide nodes als ze AutoCloseable zijn
        try (
            TwistPublisherNode pubNode = new TwistPublisherNode();
            TwistSubscriberNode subNode = new TwistSubscriberNode()
        ) {
            // 1. Start de executor voor BEIDE nodes in één of meerdere threads.
            // Omdat jouw Executor.spin() één node per keer pakt, 
            // starten we ze parallel in Virtual Threads.
            Thread pubThread = Thread.ofVirtual().start(() -> executor.spin(pubNode));
            Thread subThread = Thread.ofVirtual().start(() -> executor.spin(subNode));

            // 2. Geef de nodes de tijd om elkaar te ontdekken (Discovery)
            // In ROS 2 duurt discovery via DDS meestal 0.5 tot 2 seconden.
            Thread.sleep(2000);

            // 3. Publiceer data vanaf de publisher
            System.out.println("Versturen van test velocity...");
            pubNode.publishVelocity(0.5, 1.2);

            // 4. Wacht even zodat de subscriber de data kan verwerken
            Thread.sleep(1000);

            // 5. Shutdown en Cleanup
            RosContext.shutdown();
            
            // Wacht tot de threads netjes afsluiten
            pubThread.join(Duration.ofSeconds(2));
            subThread.join(Duration.ofSeconds(2));
            boolean received = subNode.messageReceived.await(5, TimeUnit.SECONDS);

            assertTrue(received, "Subscriber heeft geen bericht ontvangen!");
            assertEquals(0.5, subNode.lastX, 0.01);
            
            assertFalse(pubThread.isAlive(), "Publisher executor draait nog!");
            assertFalse(subThread.isAlive(), "Subscriber executor draait nog!");

        } catch (InterruptedException e) {
            fail("Test onderbroken: " + e.getMessage());
        }
    }

    @Test
    void testSpinSetBoolService() {
        System.out.println("Start Service Node");
        Executor executor = new Executor();
        try (Node node = new BoolServiceNode()) {
            // Start de spin in een aparte thread
            Thread spinThread = Thread.ofVirtual().start(() -> executor.spin(node));
            
            // Geef het even de tijd om te starten
            Thread.sleep(10000);
            
            // Trigger de shutdown
            RosContext.shutdown(); 
            
            // Wacht tot de thread stopt. Als spin() goed werkt, 
            // stopt de loop omdat rcl_context_is_valid false wordt.
            spinThread.join(Duration.ofSeconds(2));
            
            if (spinThread.isAlive()) {
                fail("Executor did not stop after context shutdown");
            }
        } catch(InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException.getMessage());
        }
    }

    @Test
    void testServiceClient() {
        Executor executor = new Executor();
        
        try (Node node = new BoolServiceNode(); BoolClientNode clientNode = new BoolClientNode()) {
            // Start de spin voor beide nodes in aparte threads
            Thread serviceThread = Thread.ofVirtual().start(() -> executor.spin(node));
            Thread clientThread = Thread.ofVirtual().start(() -> executor.spin(clientNode));
            
            // Geef het even de tijd om te starten
            Thread.sleep(2000);
            clientNode.publishBool(true);
            Thread.sleep(10000);
            
            // Trigger de shutdown
            RosContext.shutdown();
            
            // Wacht tot de threads netjes afsluiten
            serviceThread.join(Duration.ofSeconds(2));
            clientThread.join(Duration.ofSeconds(2));

            if (serviceThread.isAlive()) {
                fail("Service Node did not stop after context shutdown");
            }
            if (clientThread.isAlive()) {
                fail("Client Node did not stop after context shutdown");
            }

        } catch (InterruptedException e) {
            fail("Test onderbroken: " + e.getMessage());
        }
    }

    @Test
    void testTimerNode() {
        Executor executor = new Executor();
        
        try (Node node = new TimerNode()) {
            // Start de spin voor beide nodes in aparte threads
            Thread timerThread = Thread.ofVirtual().start(() -> executor.spin(node));
            
            // Geef het even de tijd om te starten
            Thread.sleep(5000);
            
            // Trigger de shutdown
            RosContext.shutdown();
            
            // Wacht tot de threads netjes afsluiten
            timerThread.join(Duration.ofSeconds(2));

            if (timerThread.isAlive()) {
                fail("Timer Node did not stop after context shutdown");
            }
        } catch (InterruptedException e) {
            fail("Test onderbroken: " + e.getMessage());
        }
    }
}
