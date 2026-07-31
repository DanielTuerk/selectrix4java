# Sequence Diagram: Reading and Writing the Bus (SerialDevice)

## Simplified Overview

Callers never talk to the serial port directly — they only ever send a task
(write or read) to the `BusDataChannel`. The channel executes tasks against
the device one at a time. Incoming data is handed to the consumers, and only
the consumers whose address actually changed delegate that change to their
registered listeners.

```mermaid
sequenceDiagram
    autonumber
    participant App as Application
    participant BDC as BusDataChannel
    participant Dev as Device (SerialPort)
    participant Cons as Consumers
    participant Lst as Listeners

    App->>BDC: send write task (data to send)
    App->>BDC: (implicitly) send read task
    BDC->>Dev: execute next task (write or read)
    Dev-->>BDC: bus data

    BDC->>Cons: dispatch bus data
    Cons->>Cons: compare with last known value
    opt value changed
        Cons->>Lst: notify registered listener
        Lst-->>App: value changed callback
    end
```

**Summary:**
- The application only ever sends **tasks** (write or read) — it never accesses the serial port itself.
- The `BusDataChannel` executes one task at a time and forwards the resulting bus data to the consumers.
- Consumers track the last known value per address and only delegate to their registered listeners when the value actually **changed**.

## Detailed View

The `BusDataChannel` uses a `ScheduledExecutorService` (delay: 77ms) to control
access to the `SerialPort`. If a write task is queued, it is executed. If the
queue is empty, the `ReadBlockTask` is executed instead, which reads the
complete bus 0 and bus 1 data.

```mermaid
sequenceDiagram
    autonumber
    participant App as Application
    participant BA as BusAddress
    participant BDC as BusDataChannel
    participant Q as Queue
    participant Sched as ScheduledExecutorService
    participant Exec as serialTaskExecutor
    participant WT as WriteTask
    participant RBT as ReadBlockTask
    participant SP as SerialPort
    participant BDD as BusDataDispatcher
    participant Lst as BusAddressListener

    Note over App,SP: Write operation (sendData)
    App->>BA: sendData(data) / send()
    BA->>BDC: send(BusData)
    BDC->>Q: offer(BusDataWriteTask)

    loop every 77ms (scheduleWithFixedDelay)
        Sched->>Q: poll()
        alt Queue not empty
            Q-->>Sched: WriteTask
            Sched->>Exec: submit(writeTask)
            Exec->>WT: call()
            WT->>SP: write(data)
            WT->>SP: read(buf, 250ms)
            SP-->>WT: response bytes
            WT-->>Exec: true / false
        else Queue empty -> default read
            Sched->>RBT: setReceivers(receivers)
            Sched->>Exec: submit(readBlockTask)
            Exec->>RBT: call()
            RBT->>SP: write([ADDRESS=120, DATA=3])
            RBT->>SP: read(reply[226], 1000ms)
            SP-->>RBT: 226 bytes (bus 0 + bus 1)
            RBT->>BDD: received(0, bus0Data[0..113])
            RBT->>BDD: received(1, bus1Data[113..226])
            BDD->>BDD: callConsumers(data, oldData)
            BDD->>BA: consumer.valueChanged(old, new)
            BA->>BA: lastReceivedData = new
            BA->>Lst: dataChanged(old, new)
            Lst-->>App: callback (e.g. rail voltage, system format)
        end
        Exec-->>Sched: result (true/false)
        opt Error (errorCount >= MAX_ERROR_COUNT)
            Sched->>BDC: shutdownNow()
        end
    end
```

**Summary:**
- **Write**: `BusAddress.sendData()` queues a `BusDataWriteTask` on the `BusDataChannel`. The scheduler picks it up on the next tick, writes the bytes to the `SerialPort`, and reads the acknowledgement (250ms timeout).
- **Read**: If the queue is empty, the scheduler runs the `ReadBlockTask` instead — it sends the request `[120, 3]`, reads 226 bytes (bus 0 + bus 1), and distributes them via `BusDataDispatcher` to all `AbstractBusDataConsumer`s, which in turn update the `BusAddress` objects and notify their `BusAddressListener`s.
- On too many errors (`MAX_ERROR_COUNT`), the channel is terminated via `shutdownNow()`.
