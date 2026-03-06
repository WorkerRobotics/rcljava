// package com.workerrobotics.rcljava.core;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.lang.foreign.Arena;
// import java.lang.foreign.MemorySegment;
// import java.util.function.Consumer;
// import org.junit.jupiter.api.Test;
// import org.mockito.MockedStatic;
// import org.ros2.rcl.RclLib;
// import com.workerrobotics.rcljava.loader.RosLoader;
// import com.workerrobotics.rcljava.core.clock.ClockType;

// class SubscriptionTest {

//     // Concrete implementatie voor testdoeleinden
//     static class TestNode extends Node {
//         TestNode(Arena arena, MemorySegment handle) {
//             // We roepen de private constructor aan (via reflectie) 
//             // of we voegen een protected constructor toe aan Node die direct velden accepteert.
//             // Aangezien je private constructor al bestaat, gebruiken we hier een trucje:
//             super("test_node", ClockType.RCL_STEADY_TIME); 
//             // OF: als je de Node constructor niet echt wilt uitvoeren (geen native calls):
//             // Pas de Node class aan om een constructor te hebben die Arena/Handle accepteert.
//         }
//     }

//     @Test
//     void testSubscriptionLogic() {
//         try (MockedStatic<RclLib> rclMock = mockStatic(RclLib.class);
//             MockedStatic<RosLoader> loaderMock = mockStatic(RosLoader.class);
//             Arena testArena = Arena.ofShared()) {

//             MemorySegment dummyHandle = MemorySegment.ofAddress(0x123);
            
//             // Gebruik rclMock.when(...) ALTIJD met matchers binnen de haakjes
//             rclMock.when(() -> RclLib.rcl_get_zero_initialized_node(any(Arena.class))).thenReturn(dummyHandle);
//             rclMock.when(() -> RclLib.rcl_node_get_default_options(any(Arena.class))).thenReturn(MemorySegment.NULL);

//             // rcl_node_init retourneert een int, zorg dat alle matchers kloppen
//             rclMock.when(() -> RclLib.rcl_node_init(
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class)
//             )).thenReturn(0);

//             rclMock.when(() -> RclLib.rcl_get_zero_initialized_subscription(any(Arena.class))).thenReturn(dummyHandle);
//             rclMock.when(() -> RclLib.rcl_subscription_get_default_options(any(Arena.class))).thenReturn(MemorySegment.NULL);
            
//             rclMock.when(() -> RclLib.rcl_subscription_init(
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class), 
//                 any(MemorySegment.class)
//             )).thenReturn(0);
            
//             loaderMock.when(() -> RosLoader.findSymbol(anyString())).thenReturn(MemorySegment.ofAddress(0xABC));

//             // Mock de Node (we gebruiken een mock i.p.v. een subklasse om rcl_node_init calls te vermijden)
//             Node mockNode = mock(Node.class);
//             // Omdat nodeArena en nodeHandle protected final zijn, moeten we de getters mocken 
//             // (als die er zijn) of reflectie gebruiken om ze te zetten.
//             // Als je ze niet kunt zetten, gebruik dan de TestNode van de vorige stap maar zorg 
//             // dat rcl_node_init correct gemockt is VOORDAT de constructor draait.

//             // Act
//             Subscription<String> sub = new Subscription<>(mockNode, "/test", String.class, msg -> {});

//             // Assert
//             assertEquals("/test", sub.topicName());
//             rclMock.verify(() -> RclLib.rcl_subscription_init(any(), any(), any(), any(), any()), times(1));
//         }
//     }

// }
