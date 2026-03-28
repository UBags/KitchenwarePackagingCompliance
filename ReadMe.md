# KitchenwarePackagingCompliance

A Java OCR system that reads product labels on kitchenware packaging (Prestige / Judge brand pressure cookers and cookware) and validates that the **product name**, **retail price**, and **manufacturing date** printed on the label match a known Price Master reference file.

The system watches an input folder for incoming label images, processes each image through a multi-threaded pipeline, and writes structured validation results to an output folder. A JavaFX UI displays live results and exposes Start / Stop / Report controls.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Package Structure](#package-structure)
- [Class Hierarchy](#class-hierarchy)
- [Main Processing Pipeline](#main-processing-pipeline)
- [Image Pre-processing](#image-pre-processing)
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
flowchart TD
    A[Input Folder WatchService] -->|new image| B[ReadLabelsMultiThreaded]
    B --> C[getBaseImages]
    C --> D[getDerivativeImagesAndBoundingBoxes]
    D --> E[getOCRBufferedImageWrapperArrayFast]
    E --> F[TechWerxTesseractHandlePool]
    F --> G[OCRResult]
    G --> H[ProductPriceData validate]
    H --> I[ProductDescription]
    I --> J[Resources write files]
    J --> K[JavaFX UI]
    L[Price Master CSV] --> H
    M[config.properties] --> B
    N[product.properties] --> B
```

---

## Package Structure

```mermaid
flowchart TD
    ROOT[com.techwerx]

    ROOT --> IMG[image]
    ROOT --> TXT[text]
    ROOT --> TSS[tesseract]
    ROOT --> LBL[labelprocessing]

    IMG --> IMU[image.utils]
    IMG --> IMS[image.streams]
    IMG --> IMT[image.thresholds]

    IMU --> IMU1[SBImageUtils]
    IMU --> IMU2[PixDebugWriter]
    IMU --> IMU3[SysOutController]
    IMU --> IMU4[ImageNumbers]
    IMU --> IMU5[ExclusionList]

    IMS --> IMS1[FastByteArrayInputStream]
    IMS --> IMS2[FastByteArrayOutputStream]
    IMT --> IMT1[OtsuThreshold]

    IMG --> IMG1[SBImage]
    IMG --> IMG2[BBox]
    IMG --> IMG3[Pair]
    IMG --> IMG4[PixAutoCloseable]
    IMG --> IMG5[PixCleaningUtils]

    TXT --> TXT1[OCRResult]
    TXT --> TXT2[OCRBufferedImageWrapper]
    TXT --> TXT3[OCRStringWrapper]
    TXT --> TXT4[Resources]
    TXT --> TXT5[KDEData]

    TSS --> TSS1[TechWerxTesseract]
    TSS --> TSS2[TechWerxTesseractHandle]
    TSS --> TSS3[TechWerxTesseractHandlePool]
    TSS --> TSS4[TechWerxTesseractHandleFactory]
    TSS --> TSS5[PoolHandle]

    LBL --> LBL1[OCRDimensionsWrapper]
    LBL --> LBL2[OCRPriceDimensionsWrapper]
    LBL --> LBL3[OCRDateDimensionsWrapper]
    LBL --> LBL4[OCRProductDimensionsWrapper]
    LBL --> LBL5[ProductDescription]
    LBL --> LBL6[ProcessingData]

    LBL --> PST[prestige]
    PST --> PST1[ReadLabelsMultiThreaded]
    PST --> PST2[ProductPriceData]
    PST --> PST3[CheckProductProperties]
    PST --> INI[prestige.initialise]
    INI --> INI1[InitialiseParameters]
```

---

## Class Hierarchy

```mermaid
flowchart TD
    OW[OCRDimensionsWrapper abstract]
    OW --> OP[OCRProductDimensionsWrapper]
    OW --> OD[OCRDateDimensionsWrapper]
    OW --> OPR[OCRPriceDimensionsWrapper]

    APP[javafx.application.Application]
    APP --> RLM[ReadLabelsMultiThreaded]

    PH[PoolHandle interface]
    PH --> TTH[TechWerxTesseractHandle]

    GOP[GenericObjectPool]
    GOP --> TTHP[TechWerxTesseractHandlePool]

    BPOF[BasePooledObjectFactory]
    BPOF --> TTHF[TechWerxTesseractHandleFactory]

    AL[java.util.ArrayList]
    AL --> EL[ExclusionList]

    IS[java.io.InputStream]
    IS --> FBAIS[FastByteArrayInputStream]

    OS[java.io.OutputStream]
    OS --> FBAOS[FastByteArrayOutputStream]

    AC[AutoCloseable]
    AC --> PAC[PixAutoCloseable]
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

    FS->>RLM: new image file detected
    RLM->>RLM: poll until file readable
    RLM->>RLM: pixRead and scale to 400x600
    RLM->>GBI: getBaseImages(pix)
    GBI-->>RLM: CN + BN + UM images ready
    RLM->>GD: getDerivativeImagesAndBoundingBoxes
    note over GD: try1 try2 try3 run in parallel
    GD-->>RLM: PreBBImages and BoundingBoxes
    RLM->>GO: getOCRWrapperArrayFast(pix, boxes)
    GO->>TP: borrowObject
    TP-->>GO: TesseractHandle
    GO->>GO: scale rotate assemble targetPix
    GO->>TP: returnObject
    GO-->>RLM: OCRBufferedImageWrapper list
    RLM->>PPD: validate(ocrResults)
    PPD->>PPD: month year price product matching
    PPD-->>RLM: ProductDescription
    RLM->>RES: writeResult or writeError
```

---

## Image Pre-processing

```mermaid
flowchart TD
    A[originalPix] --> B[pixConvertTo8]
    B --> C[pixUnsharpMaskingGray x3]
    C --> D{productIsGiven}
    D -->|Yes| E[pixBlockconvGray 2x1]
    D -->|No| F[pixCopy]
    E --> G[pixBackgroundNormFlex]
    F --> G
    G --> H{productIsGiven}
    H -->|Yes| I[pixContrastNorm 18px tiles]
    H -->|No| J[pixContrastNorm 24px tiles]
    I --> K[contrastNormalisedImage]
    J --> K
    G --> L[backgroundNormalisedImage]
    L --> M[pixUnsharpMaskingGray radius 5]
    M --> N[originalPix8]
    N --> O{productIsGiven}
    O -->|Yes| P[pixBlockconvGray 1x2]
    O -->|No| Q[pixCopy]
    P --> R[pixBackgroundNormFlex]
    Q --> R
    R --> S[unsharpMaskedImage]

    S --> T1[try1 pixOpenGray pixErodeGray\npixBilateralGray Sauvola 0.005]
    S --> T3[try3 pixOpenGray pixErodeGray\npixBilateralGray Sauvola 0.30]

    T1 --> U1[PixCleaningUtils removeSaltPepper\nthen removeLines]
    T3 --> U3[PixCleaningUtils removeSaltPepper\nthen removeLines]

    U1 --> V1[try1PreBBImage]
    U3 --> V3[try3PreBBImage]
```

---

## Bounding Box Pipeline

```mermaid
flowchart LR
    IN[tryN PreBBImage\n1-bpp Pix]
    IN --> R1[Round 1\ngetDefaultBoxes\nsegregateIntoLines]
    R1 --> R2[Round 2\nreallocateLines pass 1]
    R2 --> R3[Round 3\nreallocateLines pass 2]
    R3 --> R4[Round 4\nremoveLargeBoxes\nAndRedistribute]
    R4 --> R5[Round 5\nreallocateLines pass 3]
    R5 --> R6[Round 6\nreallocateLinesAgain]
    R6 --> R7[Round 7\nsplitLinesByYCoordinate]
    R7 --> R8[Round 8\nreallocateVerticalBoxes]
    R8 --> R9[Round 9\nremoveEdgeKissingBoxes]
    R9 --> OUT[ArrayList per line of Rectangles]
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

    note over RLM: Startup
    RLM->>FACT: new Factory(processInstance, debugLevel)
    RLM->>POOL: new Pool(factory, smallPoolConfig)
    POOL->>POOL: preparePool pre-create handles
    POOL->>FACT: create x minIdle
    FACT->>HDL: new TesseractHandle
    HDL->>TESS: new TechWerxTesseract

    note over RLM: Per OCR call
    RLM->>POOL: borrowObject
    POOL-->>RLM: TesseractHandle
    RLM->>HDL: getHandle doOCR image
    HDL->>TESS: doOCR image
    TESS-->>RLM: OCR text result
    RLM->>POOL: returnObject handle
    POOL->>FACT: validateObject handle
    FACT->>HDL: isValid

    note over RLM: Shutdown
    RLM->>POOL: close
    POOL->>FACT: destroyObject x all
    FACT->>HDL: destroy
    HDL->>TESS: destroy
```

---

## Validation Flow

```mermaid
flowchart TD
    A[OCRResult list] --> B[processForMonth\nFuzzyWuzzy match]
    A --> C[processForYear\n4-digit regex]
    A --> D[processForPrice\n3-digit regex]

    B --> E{month found}
    C --> F{year found}
    D --> G{price index found}

    E -->|Yes| H[monthFound true]
    E -->|No| I[monthFound false]
    F -->|Yes| J[yearFound true]
    F -->|No| K[yearFound false]
    G -->|Yes| L[priceIndices list]
    G -->|No| M[empty priceIndices]

    L --> N{productIsGiven}
    N -->|Yes| O[match givenProductPrice\nOCRPriceDimensionsWrapper\ndistance check]
    N -->|No| P[FuzzyWuzzy plus Sift4\ncapacity variant matching]

    O --> Q{price AND month\nAND year AND dot}
    P --> R{product AND month\nAND year AND dot}

    Q -->|Yes| S[ALL_OK]
    Q -->|No| T[ERROR with reason]
    R -->|Yes| S
    R -->|No| T

    S --> U{dimension checks pass}
    U -->|Yes| V[ProductDescription ALL_OK]
    U -->|No| W[ProductDescription ERROR_DIMENSION]
    T --> X[ProductDescription with rejectionReason]
```

---

## Threading Model

```mermaid
flowchart TD
    M[Main JavaFX Thread]
    M --> W[WatchService Loop]
    W --> OUT[outerThreadService\nFixedThreadPool 30]

    OUT --> T1[try1Thread\nSauvola binarise]
    OUT --> T2[try2Thread\nBG-norm binarise]
    OUT --> T3[try3Thread\nSauvola alternate]

    T1 --> BB1[getBoundingBoxes thread 1]
    T2 --> BB2[getBoundingBoxes thread 2]
    T3 --> BB3[getBoundingBoxes thread 3]

    BB1 --> INN[innerThreadService\nFixedThreadPool 60]
    BB2 --> INN
    BB3 --> INN

    INN --> O1[OCR line 1]
    INN --> O2[OCR line 2]
    INN --> ON[OCR line N]

    O1 --> TP[TesseractHandlePool\nmax 15 instances]
    O2 --> TP
    ON --> TP

    PAR[parallelThreadPool\nFixedThreadPool 10]
    PAR --> BN[BN image OCR]
    PAR --> CN[CN image OCR]
    BN --> OTP[originalImagesTesseractPool]
    CN --> OTP
```

---

## Per-Image State Machine

```mermaid
stateDiagram-v2
    [*] --> Waiting : WatchService detects file
    Waiting --> Loading : file lock released
    Waiting --> Waiting : not ready poll 5ms
    Loading --> BasePrep : pixRead and scale
    BasePrep --> DerivativePrep : CN BN UM images ready
    DerivativePrep --> BBExtraction : try threads complete
    BBExtraction --> OCRPrep : 9-round BB pipeline done
    OCRPrep --> OCRRunning : sub-images assembled
    OCRRunning --> Validation : all OCRResult collected
    Validation --> Writing : ProductDescription produced
    Writing --> [*] : result file written
    OCRRunning --> Writing : interrupted partial result
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

    UI->>RLM: Start clicked
    RLM->>FS: register input folder
    loop Watch cycle
        FS-->>RLM: WatchKey signalled
        RLM->>PAD: run resources textFlow
        PAD->>PF: processAFile
        PF->>RLM: processFileMultiThreadedFast path
        RLM-->>PF: ProductDescription
        PF->>UI: Platform.runLater update UI
        PAD-->>RLM: ProcessingData aggregate
        RLM->>FS: key.reset
    end
    UI->>RLM: Stop clicked
    RLM->>RLM: loop false shutdown pools
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

    MAIN->>IP: initialise configFile fallback
    IP->>IP: loadPropertiesFromFile
    IP->>SYS: setProperty for each entry
    IP->>IP: setupLibraryPaths
    IP->>SYS: prepend externallib to java.library.path
    IP->>IP: reflection sys_paths override
    IP->>NAT: System.load liblept
    IP->>NAT: System.load libtesseract
    IP-->>MAIN: true

    MAIN->>IP: getProductSetting product.properties
    IP->>IP: loadPropertiesFromFile
    IP->>SYS: productname.fixed and product.name
    IP->>CPP: reset
    IP->>PPD: loadMonths
    IP->>PPD: loadYears
    IP-->>MAIN: true

    MAIN->>PPD: initialise
    PPD->>PPD: loadProductsAndPrices from CSV
    PPD->>PPD: build products prices capacityVariants
    PPD->>CPP: checkIfProductIsGiven
    PPD-->>MAIN: pkInstance ready
```

---

## External Dependencies

```mermaid
flowchart LR
    APP[readPrestigeLabels]

    APP --> A[tess4j\nTesseract OCR wrapper]
    APP --> B[lept4j\nLeptonica image ops]
    APP --> C[com.sun.jna\nJava Native Access]
    APP --> D[commons.math3\nDescriptiveStatistics]
    APP --> E[commons.pool2\nGenericObjectPool]
    APP --> F[commons.io\nApache file utilities]
    APP --> G[fuzzywuzzy\nfuzzy string matching]
    APP --> H[java.string.similarity\nSift4 edit distance]
    APP --> I[jfreechart\nPDF report charts]
    APP --> J[jai.imageio.core\nTIFF read and write]
    APP --> K[jboss.vfs\nresource loading]
    APP --> L[org.slf4j\nlogging facade]
    APP --> M[javafx.controls\nJavaFX UI]
    APP --> N[java.desktop\nBufferedImage AWT]
```