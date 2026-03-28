# KitchenwarePackagingCompliance

A Java OCR system that reads product labels on kitchenware packaging (Prestige / Judge brand pressure cookers and cookware) and validates that the **product name**, **retail price**, and **manufacturing date** printed on the label match a known Price Master reference file.

The system watches an input folder for incoming label images, processes each image through a multi-threaded pipeline, and writes structured validation results to an output folder. A JavaFX UI displays live results and exposes Start / Stop / Report controls.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Package Structure](#package-structure)
- [Class Diagram](#class-diagram)
- [Inheritance Hierarchy](#inheritance-hierarchy)
- [Component Dependencies](#component-dependencies)
- [Main Processing Pipeline](#main-processing-pipeline)
- [Image Processing Detail](#image-processing-detail)
- [Bounding Box Pipeline](#bounding-box-pipeline)
- [Tesseract Pool Lifecycle](#tesseract-pool-lifecycle)
- [Validation Flow](#validation-flow)
- [Threading Model](#threading-model)
- [Per-Image State Machine](#per-image-state-machine)
- [File Watcher Loop](#file-watcher-loop)
- [Initialisation Sequence](#initialisation-sequence)
- [External Dependencies](#external-dependencies)

---

## Architecture Overview

```mermaid
graph TD
    A["Input Folder\nWatchService"] -->|"new image file"| B["ReadLabelsMultiThreaded\nJavaFX Application"]
    B --> C["Image Pre-processing\ngetBaseImages"]
    C --> D["Parallel Try-Threads\ngetDerivativeImagesAndBoundingBoxes"]
    D --> E["OCR Sub-image Extraction\ngetOCRBufferedImageWrapperArrayFast"]
    E --> F["TechWerxTesseractHandlePool\nOCR Engine Pool"]
    F --> G["OCRResult\nsentences and confidences"]
    G --> H["ProductPriceData\nvalidate"]
    H --> I{"Match found?"}
    I -->|"Yes - ALL_OK"| J["ProductDescription\nproduct + price + date"]
    I -->|"No"| K["ProductDescription\nwith rejection reason"]
    J --> L["Resources\nWrite results files"]
    K --> L
    L --> M["JavaFX UI\nLive results panel"]
    N["Price Master CSV"] -->|"loadProductsAndPrices"| H
    O["config.properties"] -->|"InitialiseParameters"| B
    P["product.properties"] -->|"CheckProductProperties"| B
```

---

## Package Structure

```mermaid
graph LR
    ROOT["com.techwerx"]
    ROOT --> IMG["image"]
    ROOT --> TEXT["text"]
    ROOT --> TESS["tesseract"]
    ROOT --> LABEL["labelprocessing"]

    IMG --> IMG_UTIL["image.utils"]
    IMG --> IMG_STREAM["image.streams"]
    IMG --> IMG_THRESH["image.thresholds"]

    IMG_UTIL --> U1["SBImageUtils"]
    IMG_UTIL --> U2["PixDebugWriter"]
    IMG_UTIL --> U3["SysOutController"]
    IMG_UTIL --> U4["ImageNumbers"]
    IMG_UTIL --> U5["ExclusionList"]
    IMG_UTIL --> U6["NumberTriplet"]

    IMG_STREAM --> S1["FastByteArrayInputStream"]
    IMG_STREAM --> S2["FastByteArrayOutputStream"]
    IMG_THRESH --> T1["OtsuThreshold"]

    IMG --> I1["SBImage"]
    IMG --> I2["BBox / Pair / DimensionScaling"]
    IMG --> I3["PixAutoCloseable"]
    IMG --> I4["PixCleaningUtils"]

    TEXT --> TX1["OCRResult"]
    TEXT --> TX2["OCRBufferedImageWrapper"]
    TEXT --> TX3["OCRStringWrapper"]
    TEXT --> TX4["Resources"]
    TEXT --> TX5["KDEData / ProcessDataWrapper"]

    TESS --> TE1["TechWerxTesseract"]
    TESS --> TE2["TechWerxTesseractHandle"]
    TESS --> TE3["TechWerxTesseractHandlePool"]
    TESS --> TE4["TechWerxTesseractHandleFactory"]
    TESS --> TE5["PoolHandle interface"]

    LABEL --> L1["OCRDimensionsWrapper abstract"]
    LABEL --> L2["OCRPriceDimensionsWrapper"]
    LABEL --> L3["OCRDateDimensionsWrapper"]
    LABEL --> L4["OCRProductDimensionsWrapper"]
    LABEL --> L5["ProductDescription"]
    LABEL --> L6["ProcessingData"]

    LABEL --> PRESTIGE["prestige"]
    PRESTIGE --> P1["ReadLabelsMultiThreaded"]
    PRESTIGE --> P2["ProductPriceData"]
    PRESTIGE --> P3["CheckProductProperties"]
    PRESTIGE --> INIT["prestige.initialise"]
    INIT --> IN1["InitialiseParameters"]
```

---

## Class Diagram

```mermaid
classDiagram
    direction TB

    class ReadLabelsMultiThreaded {
        +int debugLevel
        +ExecutorService outerThreadService
        +ExecutorService innerThreadService
        +processFileMultiThreadedFast(path) ProductDescription
        +getBaseImages(pix, debugL) void
        +getDerivativeImagesAndBoundingBoxes(debugL) void
        +getBoundingBoxes(pix, threadNum, debugL) ArrayList
        +getOCRBufferedImageWrapperArrayFast(pix, boxes) ArrayList
        +getFinalProductDescriptions(debugL) ProductDescription
    }

    class ProductPriceData {
        +HashMap productPriceMap
        +ArrayList products
        +ArrayList prices
        +ArrayList months
        +ArrayList years
        +loadProductsAndPrices() void
        +validate(ocrResults, debugL) ProductDescription
        +processForMonth(line, h, debugL) Entry
        +processForYear(line, debugL) String
        +processForPrice(line, h, boxes, idx, debugL) ArrayList
        +findProductIndexUsingFuzzyWuzzy(line, cutoff, debugL) List
    }

    class ProductDescription {
        +String fileName
        +String productName
        +ArrayList price
        +String finalPrice
        +String month
        +String year
        +int productOK
        +String rejectionReason
        +int ALL_OK$
        +int ERROR_ONLY_ONE_THING_NOT_FOUND$
        +int ERROR_DIMENSION_PROBLEM$
        +int BAD_ERROR_WILL_NEED_REPROCESSING$
    }

    class InitialiseParameters {
        +initialise() bool
        +initialise(file, fallback) bool
        +getProductSetting(file, fallback) bool
        -loadPropertiesFromFile(f, silent) Properties
        -applySystemProperties(props) String
        -setupLibraryPaths(folder) String
        -loadNativeLibraries(folder, lept, tess, ext) void
    }

    class CheckProductProperties {
        +bool productIsGiven$
        +int givenProductPrice$
        +checkIfProductIsGiven() bool
        +reset() void
    }

    class OCRDimensionsWrapper {
        <<abstract>>
        +int likelyPixelHeight
        +double likelyActualHeight
        +String ocrString
        +ArrayList boundingBoxes
        +bool heightOK
        +String reasonForRejection
        +process(basePixH, baseActualH) void
    }

    class OCRPriceDimensionsWrapper {
        +double gapBetween1And2
        +bool distanceOK
        +String reasonForDistanceRejection
        +process(basePixH, baseActualH) void
    }

    class OCRProductDimensionsWrapper {
        +process(basePixH, baseActualH) void
    }

    class OCRDateDimensionsWrapper {
        +process(basePixH, baseActualH) void
    }

    class TechWerxTesseract {
        +String datapath$
        +String language$
        +doOCR(image) String
        +isValid() bool
        +destroy() bool
    }

    class TechWerxTesseractHandle {
        -TechWerxTesseract handle
        -int processInstance
        +isValid() bool
        +release() bool
        +destroy() bool
        +getHandle() TechWerxTesseract
    }

    class TechWerxTesseractHandlePool {
        +GenericObjectPoolConfig singletonConfig$
        +GenericObjectPoolConfig defaultConfig$
        +GenericObjectPoolConfig smallPoolConfig$
        +int processInstance
    }

    class TechWerxTesseractHandleFactory {
        +bool strictChecking$
        +create() TechWerxTesseractHandle
        +validateObject(po) bool
        +destroyObject(po) void
    }

    class PoolHandle {
        <<interface>>
        +handle(object) Object
        +isValid() bool
        +release() bool
        +destroy() bool
    }

    class OCRResult {
        +ArrayList sentences
        +ArrayList confidences
        +add(words, confidence) void
    }

    class OCRStringWrapper {
        +int likelyPixelHeight
        +String ocrString
        +ArrayList boundingBoxes
    }

    class OCRBufferedImageWrapper {
        +int likelyPixelHeight
        +BufferedImage image
        +ArrayList boundingBoxes
    }

    class PixCleaningUtils {
        +removeSaltPepper(pix, debugL, dir) Pix$
        +removeLines(pix, debugL, dir, tryNo) Pix$
    }

    class PixDebugWriter {
        +writeIfDebug(debugL, threshold, pix, path) void$
    }

    class PixAutoCloseable {
        -Pix pix
        +get() Pix
        +close() void
    }

    class Resources {
        +writeRollingResult(text) void
        +writeResult(product) void
        +writeError(product) void
    }

    class ProcessDataWrapper {
        +bool linesNeededSplitting
        +KDEData kdeData
        +reset() void
    }

    OCRDimensionsWrapper <|-- OCRPriceDimensionsWrapper
    OCRDimensionsWrapper <|-- OCRProductDimensionsWrapper
    OCRDimensionsWrapper <|-- OCRDateDimensionsWrapper
    PoolHandle <|.. TechWerxTesseractHandle
    TechWerxTesseractHandlePool --|> GenericObjectPool
    TechWerxTesseractHandleFactory --|> BasePooledObjectFactory
    TechWerxTesseractHandle --> TechWerxTesseract
    TechWerxTesseractHandlePool --> TechWerxTesseractHandleFactory
    ReadLabelsMultiThreaded --> ProductPriceData
    ReadLabelsMultiThreaded --> TechWerxTesseractHandlePool
    ReadLabelsMultiThreaded --> ProductDescription
    ReadLabelsMultiThreaded --> PixCleaningUtils
    ReadLabelsMultiThreaded --> PixDebugWriter
    ReadLabelsMultiThreaded --> OCRBufferedImageWrapper
    ReadLabelsMultiThreaded --> OCRStringWrapper
    ReadLabelsMultiThreaded --> ProcessDataWrapper
    ReadLabelsMultiThreaded --> Resources
    ProductPriceData --> OCRResult
    ProductPriceData --> OCRPriceDimensionsWrapper
    ProductPriceData --> OCRProductDimensionsWrapper
    ProductPriceData --> OCRDateDimensionsWrapper
    ProductPriceData --> ProductDescription
    ProductPriceData --> CheckProductProperties
    InitialiseParameters --> CheckProductProperties
    InitialiseParameters --> ProductPriceData
```

---

## Inheritance Hierarchy

```mermaid
graph TD
    A["java.lang.Object"]
    A --> B["OCRDimensionsWrapper (abstract)"]
    B --> C["OCRProductDimensionsWrapper"]
    B --> D["OCRDateDimensionsWrapper"]
    B --> E["OCRPriceDimensionsWrapper"]
    A --> F["javafx.application.Application"]
    F --> G["ReadLabelsMultiThreaded"]
    H["PoolHandle (interface)"]
    H --> I["TechWerxTesseractHandle"]
    J["GenericObjectPool"]
    J --> K["TechWerxTesseractHandlePool"]
    L["BasePooledObjectFactory"]
    L --> M["TechWerxTesseractHandleFactory"]
    N["java.io.InputStream"]
    N --> O["FastByteArrayInputStream"]
    P["java.io.OutputStream"]
    P --> Q["FastByteArrayOutputStream"]
    R["java.util.ArrayList"]
    R --> S["ExclusionList"]
    T["java.lang.AutoCloseable"]
    T --> U["PixAutoCloseable"]
```

---

## Component Dependencies

```mermaid
graph LR
    RLM["ReadLabelsMultiThreaded"]
    RLM -->|"reads config"| IP["InitialiseParameters"]
    RLM -->|"reads product.properties"| CPP["CheckProductProperties"]
    RLM -->|"validates results"| PPD["ProductPriceData"]
    RLM -->|"borrows and returns handles"| TTHP["TechWerxTesseractHandlePool"]
    RLM -->|"writes files"| RES["Resources"]
    RLM -->|"cleans pix"| PCU["PixCleaningUtils"]
    RLM -->|"debug writes"| PDW["PixDebugWriter"]
    RLM -->|"wraps intermediates"| PAC["PixAutoCloseable"]
    TTHP -->|"creates via"| TTHF["TechWerxTesseractHandleFactory"]
    TTHF -->|"creates"| TTHL["TechWerxTesseractHandle"]
    TTHL -->|"wraps"| TTT["TechWerxTesseract"]
    TTT -->|"calls"| TESS4J["tess4j Tesseract native"]
    PPD -->|"reads"| CSV["Price Master CSV"]
    PPD -->|"uses"| FW["FuzzyWuzzy"]
    PPD -->|"uses"| S4["Sift4 similarity"]
    PPD -->|"produces"| PD["ProductDescription"]
    PPD -->|"uses"| OCRPW["OCRPriceDimensionsWrapper"]
    PPD -->|"uses"| OCRDW["OCRDateDimensionsWrapper"]
    PPD -->|"uses"| OCRPDW["OCRProductDimensionsWrapper"]
    IP -->|"loads native"| LEPT["liblept Leptonica native"]
    IP -->|"loads native"| TESS4J
    PCU -->|"calls"| LEPT
    RLM -->|"processes pixels"| SBI["SBImage"]
    SBI -->|"uses"| LEPT
```

---

## Main Processing Pipeline

```mermaid
sequenceDiagram
    participant FS as FileSystem
    participant RLM as ReadLabelsMultiThreaded
    participant GBI as getBaseImages
    participant GD as getDerivativeImages
    participant GO as getOCRWrapperArray
    participant TP as TesseractPool
    participant PPD as ProductPriceData
    participant RES as Resources

    FS->>RLM: new image detected
    RLM->>RLM: poll until file is readable
    RLM->>RLM: pixRead and scale to 400x600
    RLM->>GBI: getBaseImages(originalPix)
    GBI-->>RLM: CN + BN + UM images ready
    RLM->>GD: getDerivativeImagesAndBoundingBoxes
    note over GD: try1Thread try2Thread try3Thread run in parallel
    GD-->>RLM: PreBBImages and BoundingBoxes
    RLM->>GO: getOCRWrapperArrayFast per try-thread per line
    GO->>TP: borrowObject
    TP-->>GO: TechWerxTesseractHandle
    GO->>GO: scale rotate sub-image assemble targetPix8
    GO->>TP: returnObject handle
    GO-->>RLM: ArrayList of OCRBufferedImageWrapper
    RLM->>RLM: ocrBNandCNImages and ocrBIWrapperArray
    RLM->>PPD: validate(ocrResults)
    PPD->>PPD: processForMonth processForYear processForPrice findProduct
    PPD-->>RLM: ProductDescription
    RLM->>RES: writeResult or writeError
    RES-->>FS: results file written
```

---

## Image Processing Detail

```mermaid
flowchart TD
    A["originalPix raw input"] --> B["pixConvertTo8 8-bpp greyscale"]
    B --> C["pixUnsharpMaskingGray x3 sharpen passes"]
    C --> D{"productIsGiven?"}
    D -->|"Yes"| E["pixBlockconvGray 2x1 blur"]
    D -->|"No"| F["pixCopy"]
    E --> G["pixBackgroundNormFlex 7x7 tiles gain=160"]
    F --> G
    G --> H{"productIsGiven?"}
    H -->|"Yes"| I["pixContrastNorm 18px x h/5 tiles"]
    H -->|"No"| J["pixContrastNorm 24x24 tiles"]
    I --> K["contrastNormalisedImage"]
    J --> K
    G --> M["backgroundNormalisedImage"]
    M --> N["pixUnsharpMaskingGray radius=5"]
    N --> O["originalPix8"]
    O --> P{"productIsGiven?"}
    P -->|"Yes"| Q["pixBlockconvGray 1x2"]
    P -->|"No"| R["pixCopy"]
    Q --> S["pixBackgroundNormFlex 5x5 tiles gain=60"]
    R --> S
    S --> T["unsharpMaskedImage"]

    subgraph try1 ["Try-Thread 1 - Low Sauvola factor=0.005"]
        T --> U1["pixOpenGray"]
        U1 --> V1["pixErodeGray"]
        V1 --> W1["pixBilateralGray"]
        W1 --> X1["pixSauvolaBinarizeTiled"]
        X1 --> Y1["PixCleaningUtils removeSaltPepper"]
        Y1 --> Z1["PixCleaningUtils removeLines"]
        Z1 --> AA1["try1PreBBImage"]
    end

    subgraph try3 ["Try-Thread 3 - High Sauvola factor=0.30"]
        T --> U3["pixOpenGray"]
        U3 --> V3["pixErodeGray"]
        V3 --> W3["pixBilateralGray"]
        W3 --> X3["pixSauvolaBinarizeTiled"]
        X3 --> Y3["PixCleaningUtils removeSaltPepper"]
        Y3 --> Z3["PixCleaningUtils removeLines"]
        Z3 --> AA3["try3PreBBImage"]
    end
```

---

## Bounding Box Pipeline

```mermaid
flowchart LR
    IN["tryNPreBBImage 1-bpp cleaned Pix"]
    IN --> R1["Round 1\ngetDefaultBoxes\nsegregateBoxesIntoLines\nremoveSmallBoxes"]
    R1 --> R2["Round 2\nreallocateLines\nhorizontal alignment pass 1"]
    R2 --> R3["Round 3\nreallocateLines\nhorizontal alignment pass 2"]
    R3 --> R4["Round 4\nremoveLargeBoxesAndRedistribute"]
    R4 --> R5["Round 5\nreallocateLines\nhorizontal alignment pass 3"]
    R5 --> R6["Round 6\nreallocateLinesAgain\nfine-grained reallocation"]
    R6 --> R7["Round 7\nsplitLinesByYCoordinate"]
    R7 --> R8["Round 8\nreallocateVerticalBoxes"]
    R8 --> R9["Round 9\nremoveEdgeKissingBoundingBoxes"]
    R9 --> OUT["final bounding boxes per line"]

    style R1 fill:#d4e6f1
    style R2 fill:#d4e6f1
    style R3 fill:#d4e6f1
    style R4 fill:#f9e4b7
    style R5 fill:#d4e6f1
    style R6 fill:#d5f5e3
    style R7 fill:#f5e6d4
    style R8 fill:#f5e6d4
    style R9 fill:#fadbd8
```

---

## Tesseract Pool Lifecycle

```mermaid
sequenceDiagram
    participant RLM as ReadLabelsMultiThreaded
    participant POOL as TesseractHandlePool
    participant FACT as TesseractHandleFactory
    participant HDL as TesseractHandle
    participant TESS as TechWerxTesseract

    note over RLM: At startup
    RLM->>FACT: new TesseractHandleFactory(processInstance, debugLevel)
    RLM->>POOL: new TesseractHandlePool(factory, smallPoolConfig)
    POOL->>POOL: preparePool pre-creates minIdle handles
    POOL->>FACT: create x minIdle
    FACT->>HDL: new TechWerxTesseractHandle
    HDL->>TESS: new TechWerxTesseract

    note over RLM: Per OCR call
    RLM->>POOL: borrowObject
    POOL-->>RLM: TechWerxTesseractHandle
    RLM->>HDL: getHandle().doOCR(image)
    HDL->>TESS: doOCR(image)
    TESS-->>HDL: OCR text string
    HDL-->>RLM: OCR text string
    RLM->>POOL: returnObject(handle)
    POOL->>FACT: passivateObject - no-op Tesseract self-resets
    POOL->>FACT: validateObject(handle)
    FACT->>HDL: isValid
    HDL->>TESS: isValid
    TESS-->>FACT: true or false

    note over RLM: At shutdown
    RLM->>POOL: close
    POOL->>FACT: destroyObject x all handles
    FACT->>HDL: destroy
    HDL->>TESS: destroy
```

---

## Validation Flow

```mermaid
flowchart TD
    A["ArrayList of OCRResult raw OCR sentences"]
    A --> B["processForMonth\nFuzzyWuzzy match against months list"]
    A --> C["processForYear\nregex 4-digit match against years list"]
    A --> D["processForPrice\nregex 3+ digit match against prices list"]

    B --> E{"bestMonthMatch found?"}
    C --> F{"year found?"}
    D --> G{"price index found?"}

    E -->|"Yes"| H["monthFound = true"]
    E -->|"No"| I["monthFound = false"]
    F -->|"Yes"| J["yearFound = true"]
    F -->|"No"| K["yearFound = false"]
    G -->|"Yes"| L["priceIndices populated"]
    G -->|"No"| M["priceIndices empty"]

    L --> N{"productIsGiven?"}
    N -->|"Yes"| O["Match price against givenProductPrice\nOCRPriceDimensionsWrapper distance check"]
    N -->|"No"| P["findProductIndex FuzzyWuzzy\nSift4 distance capacity variant matching"]

    O --> Q{"price AND month AND year AND priceHasADot?"}
    P --> R{"product AND month AND year AND priceHasADot?"}

    Q -->|"Yes"| S["ALL_OK"]
    Q -->|"No"| T["ERROR_ONLY_ONE_THING_NOT_FOUND"]
    R -->|"Yes"| S
    R -->|"No"| T

    S --> U{"dimension checks pass?"}
    T --> V["ProductDescription with rejectionReason"]
    U -->|"Yes"| W["ProductDescription productOK = ALL_OK"]
    U -->|"No"| X["ProductDescription productOK = ERROR_DIMENSION_PROBLEM"]
```

---

## Threading Model

```mermaid
graph TD
    MAIN["Main Thread - JavaFX Application Thread"]
    MAIN --> WATCH["WatchService Loop\nprocessAllFilesInDirectory"]
    WATCH --> OUTER["outerThreadService\nFixedThreadPool 30"]
    OUTER --> T1["try1Thread\nCompletableFuture\nSauvola binarise"]
    OUTER --> T2["try2Thread\nCompletableFuture\nBackground-norm binarise"]
    OUTER --> T3["try3Thread\nCompletableFuture\nSauvola alt binarise"]
    T1 --> GBB1["getBoundingBoxes Thread-1"]
    T2 --> GBB2["getBoundingBoxes Thread-2"]
    T3 --> GBB3["getBoundingBoxes Thread-3"]
    GBB1 --> INNER["innerThreadService\nFixedThreadPool 60"]
    GBB2 --> INNER
    GBB3 --> INNER
    INNER --> OCR1["OCR sub-image line 1"]
    INNER --> OCR2["OCR sub-image line 2"]
    INNER --> OCRN["OCR sub-image line N"]
    OCR1 --> TPOOL["TechWerxTesseractHandlePool\nsmallPoolConfig max=15"]
    OCR2 --> TPOOL
    OCRN --> TPOOL
    TPOOL --> INST1["TechWerxTesseract Instance 1"]
    TPOOL --> INST2["TechWerxTesseract Instance 2"]
    TPOOL --> INSTN["TechWerxTesseract Instance N"]
    PARALLEL["parallelThreadPool\nFixedThreadPool 10"]
    PARALLEL --> BN["ocrBNandCNImages\nbackgroundNorm OCR"]
    PARALLEL --> CN["originalImages OCR"]
    BN --> ORIG_POOL["originalImagesTesseractPool\nuseAutoOSD=true"]
    CN --> ORIG_POOL
```

---

## Per-Image State Machine

```mermaid
stateDiagram-v2
    [*] --> WaitingForFile : WatchService detects new file
    WaitingForFile --> LoadingImage : file lock released
    WaitingForFile --> WaitingForFile : file not ready yet polling 5ms
    LoadingImage --> BaseImagePrep : pixRead and scale to target size
    BaseImagePrep --> DerivativePrep : CN BN UM images ready
    DerivativePrep --> BBExtraction : try1 try2 try3 threads complete
    BBExtraction --> OCRPrep : 9-round BB pipeline complete
    OCRPrep --> TesseractOCR : sub-images assembled
    TesseractOCR --> Validation : OCRResult collected from all threads
    Validation --> Writing : ProductDescription produced
    Writing --> [*] : results file written
    TesseractOCR --> Writing : interrupted return partial result
```

---

## File Watcher Loop

```mermaid
sequenceDiagram
    participant UI as JavaFX UI
    participant RLM as ReadLabelsMultiThreaded
    participant FS as WatchService
    participant PAD as processAllFilesInDirectory
    participant PF as processAFile

    UI->>RLM: Start button clicked
    RLM->>FS: register input folder STANDARD_MODIFY and CREATE
    loop Watch cycle
        FS-->>RLM: WatchKey signalled
        RLM->>PAD: processAllFilesInDirectory(resources, textFlow, processingData)
        PAD->>PF: processAFile(resources, textFlow)
        PF->>RLM: processFileMultiThreadedFast(path)
        RLM-->>PF: ProductDescription
        PF->>UI: Platform.runLater update TextFlow
        PF->>PAD: ProcessingData files time ok
        PAD-->>RLM: aggregate ProcessingData
        alt autoReport is true
            RLM->>UI: render report line
        end
        RLM->>FS: key.reset
    end
    UI->>RLM: Stop button clicked
    RLM->>RLM: loop false shutdown thread pools
```

---

## Initialisation Sequence

```mermaid
sequenceDiagram
    participant MAIN as main
    participant IP as InitialiseParameters
    participant SYS as System Properties
    participant NAT as Native Libraries
    participant PPD as ProductPriceData
    participant CPP as CheckProductProperties

    MAIN->>IP: initialise(configFile, fallback)
    IP->>IP: loadPropertiesFromFile config.properties
    IP->>SYS: setProperty for each entry
    IP->>IP: applySystemProperties then setupLibraryPaths
    IP->>SYS: java.library.path prepend externallib.folder
    IP->>IP: reflection hack sys_paths override
    IP->>NAT: System.load liblept.so
    IP->>NAT: System.load libtesseract.so
    IP-->>MAIN: true

    MAIN->>IP: getProductSetting(product.properties, fallback)
    IP->>IP: loadPropertiesFromFile product.properties
    IP->>SYS: productname.fixed and product.name
    IP->>CPP: reset
    IP->>PPD: loadMonths
    IP->>PPD: loadYears
    IP-->>MAIN: true

    MAIN->>PPD: initialise
    PPD->>PPD: loadProductsAndPrices read Price Master CSV
    PPD->>PPD: build products prices capacityVariants uniqueProductStrings
    PPD->>CPP: checkIfProductIsGiven
    CPP-->>PPD: productIsGiven flag set
    PPD-->>MAIN: singleton pkInstance ready
```

---

## External Dependencies

```mermaid
graph LR
    APP["readPrestigeLabels module"]
    APP -->|"OCR engine"| A["tess4j - Java wrapper for Tesseract"]
    APP -->|"image processing"| B["lept4j - Java wrapper for Leptonica"]
    APP -->|"native loading"| C["com.sun.jna - Java Native Access"]
    APP -->|"statistics"| D["commons.math3 - DescriptiveStatistics"]
    APP -->|"object pooling"| E["commons.pool2 - GenericObjectPool"]
    APP -->|"file utilities"| F["commons.io - Apache Commons IO"]
    APP -->|"fuzzy matching"| G["fuzzywuzzy - string similarity"]
    APP -->|"edit distance"| H["java.string.similarity - Sift4"]
    APP -->|"charting"| I["jfreechart - PDF output charts"]
    APP -->|"TIFF image I/O"| J["jai.imageio.core - TIFF read and write"]
    APP -->|"resource loading"| K["jboss.vfs"]
    APP -->|"logging"| L["org.slf4j - logging facade"]
    APP -->|"UI"| M["javafx.controls and javafx.graphics"]
    APP -->|"AWT"| N["java.desktop - BufferedImage"]
```