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
- [State Machine — Per-Image Processing](#state-machine--per-image-processing)
- [File Watcher Loop](#file-watcher-loop)
- [Initialisation Sequence](#initialisation-sequence)
- [External Dependencies](#external-dependencies)

---

## Architecture Overview

```mermaid
graph TD
    A[Input Folder\nWatchService] -->|new image file| B[ReadLabelsMultiThreaded\nJavaFX Application]
    B --> C[Image Pre-processing\ngetBaseImages]
    C --> D[Parallel Try-Threads\ngetDerivativeImages\nAndBoundingBoxes]
    D --> E[OCR Sub-image Extraction\ngetOCRBufferedImageWrapperArrayFast]
    E --> F[TechWerxTesseractHandlePool\nOCR Engine Pool]
    F --> G[OCRResult\nsentences + confidences]
    G --> H[ProductPriceData\nvalidate]
    H --> I{Match found?}
    I -->|Yes - ALL_OK| J[ProductDescription\nproduct + price + date]
    I -->|No| K[ProductDescription\nwith rejection reason]
    J --> L[Resources\nWrite results files]
    K --> L
    L --> M[JavaFX UI\nLive results panel]

    N[Price Master CSV] -->|loadProductsAndPrices| H
    O[config.properties] -->|InitialiseParameters| B
    P[product.properties] -->|CheckProductProperties| B
```

---

## Package Structure

```mermaid
graph LR
    ROOT[com.techwerx]

    ROOT --> IMG[image]
    ROOT --> TEXT[text]
    ROOT --> TESS[tesseract]
    ROOT --> LABEL[labelprocessing]
    ROOT --> PDF[pdf]

    IMG --> IMG_UTIL[utils]
    IMG --> IMG_STREAM[streams]
    IMG --> IMG_THRESH[thresholds]

    IMG_UTIL --> SBImageUtils
    IMG_UTIL --> SBImageArrayUtils
    IMG_UTIL --> PixDebugWriter
    IMG_UTIL --> SysOutController
    IMG_UTIL --> ImageNumbers
    IMG_UTIL --> ExclusionList
    IMG_UTIL --> NumberTriplet

    IMG_STREAM --> FastByteArrayInputStream
    IMG_STREAM --> FastByteArrayOutputStream
    IMG_STREAM --> FastByteArrayInputStreamData

    IMG_THRESH --> OtsuThreshold

    IMG --> SBImage
    IMG --> BBox
    IMG --> Pair
    IMG --> DimensionScaling
    IMG --> XYDivisions
    IMG --> SBSubImageCoordinates
    IMG --> DeSkewSBImage
    IMG --> PixAutoCloseable
    IMG --> PixCleaningUtils

    TEXT --> OCRResult
    TEXT --> OCRBufferedImageWrapper
    TEXT --> OCRStringWrapper_t[OCRStringWrapper]
    TEXT --> BIWrapperForOCR
    TEXT --> KDEData
    TEXT --> ProcessDataWrapper
    TEXT --> Resources

    TESS --> TechWerxTesseract
    TESS --> TechWerxTesseractHandle
    TESS --> TechWerxTesseractHandleFactory
    TESS --> TechWerxTesseractHandlePool
    TESS --> PoolHandle

    LABEL --> OCRDimensionsWrapper
    LABEL --> OCRPriceDimensionsWrapper
    LABEL --> OCRProductDimensionsWrapper
    LABEL --> OCRDateDimensionsWrapper
    LABEL --> OCRStringWrapper_l[OCRStringWrapper]
    LABEL --> ProductDescription
    LABEL --> ProcessingData

    LABEL --> PRESTIGE[prestige]
    PRESTIGE --> ReadLabelsMultiThreaded
    PRESTIGE --> ProductPriceData
    PRESTIGE --> CheckProductProperties

    PRESTIGE --> INIT[initialise]
    INIT --> InitialiseParameters

    PDF --> PDFHandlerChartFactory
```

---

## Class Diagram

```mermaid
classDiagram
    direction TB

    class ReadLabelsMultiThreaded {
        +Application
        +int debugLevel
        +ExecutorService outerThreadService
        +ExecutorService innerThreadService
        +processFileMultiThreadedFast(path) ProductDescription
        +getBaseImages(pix, debugL) void
        +getDerivativeImagesAndBoundingBoxes(debugL) void
        +getBoundingBoxes(pix, threadNum, debugL) ArrayList
        +getOCRBufferedImageWrapperArrayFast(...) ArrayList
        +getFinalProductDescriptions(debugL) ProductDescription
        +ocrBNandCNImages(debugL) ArrayList
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
        +int type
        +double minimumAllowedHeight
        +bool heightOK
        +String reasonForRejection
        +process(basePixH, baseActualH)* void
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
        +doOCR(image, rect) String
        +isValid() bool
        +destroy() bool
    }

    class TechWerxTesseractHandle {
        -TechWerxTesseract handle
        -int processInstance
        +handle(object) Object
        +isValid() bool
        +release() bool
        +destroy() bool
        +getHandle() TechWerxTesseract
    }

    class TechWerxTesseractHandlePool {
        +GenericObjectPoolConfig singletonConfig$
        +GenericObjectPoolConfig defaultConfig$
        +GenericObjectPoolConfig oneMachineConfig$
        +GenericObjectPoolConfig smallPoolConfig$
        +int processInstance
    }

    class TechWerxTesseractHandleFactory {
        +bool strictChecking$
        +create() TechWerxTesseractHandle
        +wrap(handle) PooledObject
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

    class SBImage {
        +int[][] pixels
        +int width
        +int height
        +ArrayList subImageCoordinates
        +BufferedImage underlyingBuffImage
        +getPixFromBufferedImage(bi) Pix$
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
        +moveToErrorFolder(file) void
    }

    class OtsuThreshold {
        +getThreshold(pixels) int$
        +binarise(image) int[][]$
    }

    class ProcessDataWrapper {
        +bool linesNeededSplitting
        +bool linesNotSplitDueToHighOverlap
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
    ReadLabelsMultiThreaded --> SBImage
    ProductPriceData --> OCRResult
    ProductPriceData --> OCRPriceDimensionsWrapper
    ProductPriceData --> OCRProductDimensionsWrapper
    ProductPriceData --> OCRDateDimensionsWrapper
    ProductPriceData --> ProductDescription
    ProductPriceData --> CheckProductProperties
    InitialiseParameters --> CheckProductProperties
    InitialiseParameters --> ProductPriceData
    PixAutoCloseable --> Pix
```

---

## Inheritance Hierarchy

```mermaid
graph TD
    A[java.lang.Object]

    A --> B[OCRDimensionsWrapper\nabstract]
    B --> C[OCRProductDimensionsWrapper]
    B --> D[OCRDateDimensionsWrapper]
    B --> E[OCRPriceDimensionsWrapper]

    A --> F[javafx.application.Application]
    F --> G[ReadLabelsMultiThreaded]

    H[PoolHandle\ninterface]
    H --> I[TechWerxTesseractHandle]

    J[GenericObjectPool]
    J --> K[TechWerxTesseractHandlePool]

    L[BasePooledObjectFactory]
    L --> M[TechWerxTesseractHandleFactory]

    N[java.io.InputStream]
    N --> O[FastByteArrayInputStream]

    P[java.io.OutputStream]
    P --> Q[FastByteArrayOutputStream]

    R[java.util.ArrayList]
    R --> S[ExclusionList]

    T[java.lang.AutoCloseable]
    T --> U[PixAutoCloseable]
```

---

## Component Dependencies

```mermaid
graph LR
    RLM[ReadLabelsMultiThreaded]

    RLM -->|reads config| IP[InitialiseParameters]
    RLM -->|reads product.properties| CPP[CheckProductProperties]
    RLM -->|validates results| PPD[ProductPriceData]
    RLM -->|borrows/returns handles| TTHP[TechWerxTesseractHandlePool]
    RLM -->|writes files| RES[Resources]
    RLM -->|cleans pix| PCU[PixCleaningUtils]
    RLM -->|debug writes| PDW[PixDebugWriter]
    RLM -->|wraps intermediates| PAC[PixAutoCloseable]

    TTHP -->|creates via| TTHF[TechWerxTesseractHandleFactory]
    TTHF -->|creates| TTHL[TechWerxTesseractHandle]
    TTHL -->|wraps| TTT[TechWerxTesseract]
    TTT -->|calls| TESS4J[tess4j / Tesseract native]

    PPD -->|reads| CSV[Price Master CSV]
    PPD -->|uses| FW[FuzzyWuzzy]
    PPD -->|uses| S4[Sift4 similarity]
    PPD -->|produces| PD[ProductDescription]
    PPD -->|uses wrappers| OCRPW[OCRPriceDimensionsWrapper]
    PPD -->|uses wrappers| OCRDW[OCRDateDimensionsWrapper]
    PPD -->|uses wrappers| OCRPDW[OCRProductDimensionsWrapper]

    IP -->|loads native| LEPT[liblept / Leptonica native]
    IP -->|loads native| TESS4J

    PCU -->|calls| LEPT
    PDW -->|calls| LEPT

    RLM -->|processes pixels| SBI[SBImage]
    SBI -->|uses| LEPT
```

---

## Main Processing Pipeline

```mermaid
sequenceDiagram
    participant FS as FileSystem\nWatchService
    participant RLM as ReadLabelsMultiThreaded
    participant GBI as getBaseImages
    participant GDIB as getDerivativeImages\nAndBoundingBoxes
    participant GOCR as getOCRBufferedImage\nWrapperArrayFast
    participant TPOOL as TesseractHandlePool
    participant PPD as ProductPriceData
    participant RES as Resources

    FS->>RLM: new image detected
    RLM->>RLM: wait until file is readable\n(polling loop)
    RLM->>RLM: pixRead / load image
    RLM->>RLM: scale down to ~400×600px
    RLM->>GBI: getBaseImages(originalPix)
    GBI-->>RLM: contrastNormalisedImage\nbackgroundNormalisedImage\nunsharpMaskedImage
    RLM->>GDIB: getDerivativeImagesAndBoundingBoxes()
    Note over GDIB: try1Thread, try2Thread, try3Thread\nrun in parallel
    GDIB-->>RLM: try1/2/3 PreBBImages\ntry1/2/3 BBImages\ntry1/2/3 BoundingBoxes
    RLM->>GOCR: getOCRBufferedImageWrapperArrayFast\n(per try-thread, per line)
    GOCR->>TPOOL: borrowObject()
    TPOOL-->>GOCR: TechWerxTesseractHandle
    GOCR->>GOCR: scale + rotate sub-image\nassemble targetPix8
    GOCR->>TPOOL: returnObject(handle)
    GOCR-->>RLM: ArrayList<OCRBufferedImageWrapper>
    RLM->>RLM: ocrBNandCNImages()\nocrBIWrapperArray()
    RLM->>PPD: validate(ocrResults)
    PPD->>PPD: processForMonth\nprocessForYear\nprocessForPrice\nfindProductIndexUsingFuzzyWuzzy
    PPD-->>RLM: ProductDescription
    RLM->>RES: writeResult / writeError
    RES-->>FS: results file written
```

---

## Image Processing Detail

```mermaid
flowchart TD
    A[originalPix\nRaw input image] --> B[pixConvertTo8\n8-bpp greyscale]
    B --> C[pixUnsharpMaskingGray ×3\nSharpen pass 1, 2, 3]
    C --> D{productIsGiven?}
    D -->|Yes| E[pixBlockconvGray\n2×1 blur]
    D -->|No| F[pixCopy]
    E --> G[pixBackgroundNormFlex\n7×7 tiles, gain=160]
    F --> G
    G --> H{productIsGiven?}
    H -->|Yes| I[pixContrastNorm\n18px × h/5 tiles]
    H -->|No| J[pixContrastNorm\n24×24 tiles]
    I --> K[contrastNormalisedImage]
    J --> K
    K --> L[pixAddBorder 2px white]
    G --> M[pixCopy → backgroundNormalisedImage]
    M --> N[pixUnsharpMaskingGray radius=5]
    N --> O[originalPix8]
    O --> P{productIsGiven?}
    P -->|Yes| Q[pixBlockconvGray 1×2]
    P -->|No| R[pixCopy]
    Q --> S[pixBackgroundNormFlex\n5×5 tiles, gain=60]
    R --> S
    S --> T[unsharpMaskedImage]

    subgraph try1 [Try-Thread 1 — Low Sauvola]
        T --> U1[pixOpenGray]
        U1 --> V1[pixErodeGray]
        V1 --> W1[pixBilateralGray]
        W1 --> X1[pixSauvolaBinarizeTiled\nfactor=0.005–0.30]
        X1 --> Y1[PixCleaningUtils\nremoveSaltPepper]
        Y1 --> Z1[PixCleaningUtils\nremoveLines]
        Z1 --> AA1[try1PreBBImage]
    end

    subgraph try3 [Try-Thread 3 — High Sauvola]
        T --> U3[pixOpenGray]
        U3 --> V3[pixErodeGray]
        V3 --> W3[pixBilateralGray]
        W3 --> X3[pixSauvolaBinarizeTiled\nfactor=0.30]
        X3 --> Y3[PixCleaningUtils\nremoveSaltPepper]
        Y3 --> Z3[PixCleaningUtils\nremoveLines]
        Z3 --> AA3[try3PreBBImage]
    end
```

---

## Bounding Box Pipeline

```mermaid
flowchart LR
    IN[tryNPreBBImage\n1-bpp cleaned Pix]

    IN --> R1[Round 1\ngetDefaultBoxes\nsegregateBoxesIntoLines\nremoveSmallBoxes]
    R1 --> R2[Round 2\nreallocateLines\nhorizontal alignment pass 1]
    R2 --> R3[Round 3\nreallocateLines\nhorizontal alignment pass 2]
    R3 --> R4[Round 4\nremoveLargeBoxes\nAndRedistribute]
    R4 --> R5[Round 5\nreallocateLines\nhorizontal alignment pass 3]
    R5 --> R6[Round 6\nreallocateLinesAgain\nfine-grained reallocation]
    R6 --> R7[Round 7\nsplitLinesByYCoordinate\nY-axis splitting]
    R7 --> R8[Round 8\nreallocateVerticalBoxes\nvertical box merging]
    R8 --> R9[Round 9\nremoveEdgeKissing\nBoundingBoxes\nclip border artifacts]
    R9 --> OUT[ArrayList of ArrayList of Rectangle\nfinal bounding boxes per line]

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
    participant POOL as TechWerxTesseractHandlePool
    participant FACT as TechWerxTesseractHandleFactory
    participant HDL as TechWerxTesseractHandle
    participant TESS as TechWerxTesseract

    Note over RLM: At startup
    RLM->>FACT: new TechWerxTesseractHandleFactory\n(processInstance, debugLevel)
    RLM->>POOL: new TechWerxTesseractHandlePool\n(factory, smallPoolConfig)
    POOL->>POOL: preparePool()\npre-creates minIdle handles
    POOL->>FACT: create() × minIdle
    FACT->>HDL: new TechWerxTesseractHandle()
    HDL->>TESS: new TechWerxTesseract()

    Note over RLM: Per OCR call
    RLM->>POOL: borrowObject()
    POOL-->>RLM: TechWerxTesseractHandle
    RLM->>HDL: getHandle().doOCR(image)
    HDL->>TESS: doOCR(image)
    TESS-->>HDL: OCR text string
    HDL-->>RLM: OCR text string
    RLM->>POOL: returnObject(handle)
    POOL->>FACT: passivateObject(handle)\n[no-op — Tesseract self-resets]
    POOL->>FACT: validateObject(handle)
    FACT->>HDL: isValid()
    HDL->>TESS: isValid()
    TESS-->>FACT: true/false

    Note over RLM: At shutdown
    RLM->>POOL: close()
    POOL->>FACT: destroyObject(handle) × all
    FACT->>HDL: destroy()
    HDL->>TESS: destroy()
```

---

## Validation Flow

```mermaid
flowchart TD
    A[ArrayList of OCRResult\nraw OCR sentences] --> B[processForMonth\nFuzzyWuzzy match\nagainst months list]
    A --> C[processForYear\nregex \\d4 match\nagainst years list]
    A --> D[processForPrice\nregex \\d3+ match\nagainst prices list]

    B --> E{bestMonthMatch\nfound?}
    C --> F{year found?}
    D --> G{price index\nfound?}

    E -->|Yes| H[monthFound = true]
    E -->|No| I[monthFound = false]
    F -->|Yes| J[yearFound = true]
    F -->|No| K[yearFound = false]
    G -->|Yes| L[priceIndices populated]
    G -->|No| M[priceIndices empty]

    L --> N{productIsGiven?}
    N -->|Yes| O[Match price against\ngivenProductPrice\nOCRPriceDimensionsWrapper\ndistance check]
    N -->|No| P[findProductIndexUsing\nFuzzyWuzzy\nSift4 distance\ncapacity variant matching]

    O --> Q{priceFound\n&& monthFound\n&& yearFound\n&& priceHasADot?}
    P --> R{productFound\n&& monthFound\n&& yearFound\n&& priceHasADot?}

    Q -->|Yes| S[ALL_OK]
    Q -->|No| T[ERROR_ONLY_ONE\nTHING_NOT_FOUND]
    R -->|Yes| S
    R -->|No| T

    S --> U{dimension\nchecks pass?}
    T --> V[ProductDescription\nwith rejectionReason]
    U -->|Yes| W[ProductDescription\nproductOK = ALL_OK]
    U -->|No| X[ProductDescription\nproductOK = ERROR_DIMENSION\n_PROBLEM]
```

---

## Threading Model

```mermaid
graph TD
    MAIN[Main Thread\nJavaFX Application Thread]

    MAIN --> WATCH[WatchService Loop\nprocessAllFilesInDirectory]

    WATCH --> OUTER[outerThreadService\nFixedThreadPool-30]

    OUTER --> T1[try1Thread\nCompletableFuture\nSauvola binarise]
    OUTER --> T2[try2Thread\nCompletableFuture\nBackground-norm binarise]
    OUTER --> T3[try3Thread\nCompletableFuture\nSauvola alt binarise]

    T1 --> GBB1[getBoundingBoxes\nThread-1]
    T2 --> GBB2[getBoundingBoxes\nThread-2]
    T3 --> GBB3[getBoundingBoxes\nThread-3]

    GBB1 --> INNER[innerThreadService\nFixedThreadPool-60]
    GBB2 --> INNER
    GBB3 --> INNER

    INNER --> OCR1[OCR sub-image line 1]
    INNER --> OCR2[OCR sub-image line 2]
    INNER --> OCRN[OCR sub-image line N]

    OCR1 --> TPOOL[TechWerxTesseractHandlePool\nsmallPoolConfig max=15]
    OCR2 --> TPOOL
    OCRN --> TPOOL

    TPOOL --> INST1[TechWerxTesseract\nInstance 1]
    TPOOL --> INST2[TechWerxTesseract\nInstance 2]
    TPOOL --> INSTN[TechWerxTesseract\nInstance N]

    PARALLEL[parallelThreadPool\nFixedThreadPool-10] --> BN[ocrBNandCNImages\nbackgroundNorm OCR]
    PARALLEL --> CN[originalImages OCR]
    BN --> ORIG_POOL[originalImagesTesseract\nPool useAutoOSD=true]
    CN --> ORIG_POOL
```

---

## State Machine — Per-Image Processing

```mermaid
stateDiagram-v2
    [*] --> WaitingForFile : WatchService detects new file

    WaitingForFile --> LoadingImage : file lock released\n(isFileReadyForReading)
    LoadingImage --> BaseImagePrep : pixRead / ImageIO.read\nscale to target size
    BaseImagePrep --> DerivativePrep : getBaseImages complete\nCN + BN + UM images ready
    DerivativePrep --> BBExtraction : try1/2/3 threads complete\nPreBBImages ready
    BBExtraction --> OCRPrep : 9-round BB pipeline complete\nper-line bounding boxes ready
    OCRPrep --> TesseractOCR : sub-images assembled\ntargetPix8 ready per line
    TesseractOCR --> Validation : OCRResult collected\nfrom all try-threads
    Validation --> Writing : ProductDescription produced
    Writing --> [*] : results file written\nmove to output / error folder

    WaitingForFile --> WaitingForFile : file not ready yet\n(polling 5ms)
    DerivativePrep --> DerivativePrep : interrupt check\nbetween each Leptonica call
    TesseractOCR --> Writing : interrupted\nreturn partial result
```

---

## File Watcher Loop

```mermaid
sequenceDiagram
    participant UI as JavaFX UI
    participant RLM as ReadLabelsMultiThreaded
    participant FS as FileSystem\nWatchService
    participant PAD as processAllFiles\nInDirectory
    participant PF as processAFile

    UI->>RLM: Start button clicked
    RLM->>FS: register input folder\nSTANDARD_MODIFY + CREATE
    loop Watch cycle
        FS-->>RLM: WatchKey signalled
        RLM->>PAD: processAllFilesInDirectory\n(resources, textFlow, processingData)
        PAD->>PF: processAFile(resources, textFlow)
        PF->>RLM: processFileMultiThreadedFast(path)
        RLM-->>PF: ProductDescription
        PF->>UI: Platform.runLater\nupdate TextFlow
        PF->>PAD: ProcessingData (files, time, ok)
        PAD-->>RLM: aggregate ProcessingData
        alt autoReport = true
            RLM->>UI: render report line
        end
        RLM->>FS: key.reset()
    end
    UI->>RLM: Stop button clicked
    RLM->>RLM: loop = false\nshutdown thread pools
```

---

## Initialisation Sequence

```mermaid
sequenceDiagram
    participant MAIN as main()
    participant IP as InitialiseParameters
    participant SYS as System Properties
    participant JNA as JNA / Native Libs
    participant PPD as ProductPriceData
    participant CPP as CheckProductProperties

    MAIN->>IP: initialise(configFile, fallback)
    IP->>IP: loadPropertiesFromFile\n(config.properties)
    IP->>SYS: setProperty for each entry\n(input.folder, output.folder,\ntesseract.datapath, etc.)
    IP->>IP: applySystemProperties\n→ setupLibraryPaths
    IP->>SYS: java.library.path prepend\nexternallib.folder
    IP->>IP: reflection hack\nsys_paths override
    IP->>JNA: System.load(liblept.so)
    IP->>JNA: System.load(libtesseract.so)
    IP-->>MAIN: true

    MAIN->>IP: getProductSetting\n(product.properties, fallback)
    IP->>IP: loadPropertiesFromFile\n(product.properties)
    IP->>SYS: productname.fixed\nproduct.name
    IP->>CPP: reset()
    IP->>PPD: loadMonths()
    IP->>PPD: loadYears()
    IP-->>MAIN: true

    MAIN->>PPD: initialise()
    PPD->>PPD: loadProductsAndPrices()\nread Price Master CSV
    PPD->>PPD: build products / prices\ncapacityVariants / uniqueProductStrings
    PPD->>CPP: checkIfProductIsGiven()
    CPP-->>PPD: productIsGiven flag set
    PPD-->>MAIN: singleton pkInstance ready
```

---

## External Dependencies

```mermaid
graph LR
    APP[readPrestigeLabels\nmodule]

    APP -->|OCR engine| TESS4J[tess4j\nJava wrapper for Tesseract]
    APP -->|image processing| LEPT4J[lept4j\nJava wrapper for Leptonica]
    APP -->|native loading| JNA[com.sun.jna\nJava Native Access]
    APP -->|statistics| MATH3[commons.math3\nDescriptiveStatistics]
    APP -->|object pooling| POOL2[commons.pool2\nGenericObjectPool]
    APP -->|file utilities| COMMONS_IO[commons.io\nApache Commons IO]
    APP -->|fuzzy matching| FUZZY[fuzzywuzzy\nstring similarity]
    APP -->|edit distance| SIFT4[java.string.similarity\nSift4]
    APP -->|charting / PDF| JFREE[jfreechart\nPDF output charts]
    APP -->|TIFF image I/O| JAI[jai.imageio.core\nTIFF read/write]
    APP -->|virtual filesystem| JBOSS[jboss.vfs\nresource loading]
    APP -->|logging| SLF4J[org.slf4j\nlogging facade]
    APP -->|UI| JAVAFX[javafx.controls\njavafx.graphics]
    APP -->|AWT/Swing| DESKTOP[java.desktop\nBufferedImage etc.]
```