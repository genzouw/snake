package snake

import javax.swing.*
import javax.swing.filechooser.*
import au.com.bytecode.opencsv.*
import groovy.util.logging.Slf4j

@Slf4j
class SnakeController {
    // these will be injected by Griffon
    def model
    def view
    def builder

    final def DEFAULT_FILE_ENCODING = "windows-31j"

    final def DEFAULT_FILE_TOKEN = ","

    def onStartupEnd = { evt = null ->
        log.info "onStartupEnd - start."

        try {
            def params = model.properties.clone()

            doOutside {
                log.info "application start."

                doLater {
                    def rootdir = new File(System.properties["user.home"] + "/Desktop/AltrecGetter/Definitions/Snake").absolutePath
//                    def pwd = new File(".").absoluteFile.parent
                    model.with{
                        targetFilePath = new File(rootdir + "/").absolutePath
                        brandFilePath = new File(rootdir + "/ブランドコード定義ファイル.csv").absolutePath
                        htmlFilePath = new File(rootdir + "/HTMLデータ定義ファイル.csv").absolutePath
                        itemTitleTransFilePath = new File(rootdir + "/商品タイトル翻訳用ファイル.csv").absolutePath
                        categoryFilePath = new File(rootdir + "/カテゴリ関連商品定義ファイル.csv").absolutePath
                        shoesSizeFilePath = new File(rootdir + "/靴サイズ定義ファイル.csv").absolutePath
                        productCodeFilePath = new File(rootdir + "/プロダクトコード定義ファイル.csv").absolutePath
                        itemPriceFilePath = new File(rootdir + "/商品価格定義ファイル.csv").absolutePath
                    }
                }
            }
        } catch ( e ) {
            log.error "onStartupEnd - exception.", e
        } finally {
            log.info "onStartupEnd - end."
        }
    }

    def setRelativeItems = { evt = null ->
        log.info "setRelativeItems - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                // 出力
                // サブカテゴリをキーとしてグルーピング
                def categorizedMap = createCategoriesMap( rows )

                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())
                        
                        // 関連商品情報置き直し
                        def categories = pickupCategories(row)

                        if (row.size() > 16 && !categories.empty) {
                            def itemCodesInSameCategories = pickupItemCodesInSameCategories( categories, categorizedMap )
                            def itemCode = row[2]
                            def itemCodesText = itemCodesInSameCategories.findAll {
                                it != itemCode
                            }.join(" ")
                            row[15] = itemCodesText
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setRelativeItems - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setRelativeItems - end."
        }
    }

    Map<String, List<String>> createCategoriesMap( rows ) {
        def categorizedMap = [:]
        rows.each{ row ->
            def itemCode = row[2]
            pickupCategories(row).each{
                if (!categorizedMap[it]) {
                    categorizedMap[it] = [] as Set
                }
                categorizedMap[it] << itemCode
            }
        }
        return categorizedMap
    }

    List<String> pickupCategories( row ) {
        if ( !row[0] )
            return []

        def categories = row[0].split("\n").findAll {
            it.contains(":")
        }.collect {
            it.replaceAll(/^([^:]+:[^:]+).*$/, "\$1")
        }.unique()

        return categories ?: []
    }


    List<String> pickupItemCodesInSameCategories( List<String> categories, Map<String, List<String>> categorizedMap ) {
        def itemCodesInSameCategories = []
        categories.each{
            itemCodesInSameCategories.addAll( categorizedMap.get(it) ?: [] )
        }
        itemCodesInSameCategories = itemCodesInSameCategories.unique()

        Collections.shuffle(itemCodesInSameCategories)
        return itemCodesInSameCategories.take(20)
    }

    def setRelativeItemsToCategoryFile = { evt = null ->
        log.info "setRelativeItemsToCategoryFile - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                if ( !params.categoryFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "カテゴリ関連商品定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                def categoryFile = new File("${params.categoryFilePath}")
                if (!( categoryFile.file && categoryFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "カテゴリ関連商品定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                // カテゴリ
                def rowsInCategoryFile = []
                categoryFile.withReader(DEFAULT_FILE_ENCODING) {
                    rowsInCategoryFile.addAll new CSVReader(it).readAll()
                }

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${categoryFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                categoryFile.renameTo(backupFile)
                    
                // 出力
                // サブカテゴリをキーとしてグルーピング
                def categorizedMap = createCategoriesMap( rows )

                categoryFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rowsInCategoryFile.head() as String[])
                    
                    def tails = rowsInCategoryFile.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rowsInCategoryFile.size())
                        
                        // 関連商品情報置き直し
                        def categories = pickupCategories( row )

                        if (row.size() > 4 && !categories.empty) {
                            def itemCodesInSameCategories = pickupItemCodesInSameCategories( categories, categorizedMap )
                            def itemCodesText = itemCodesInSameCategories.join(" ")
                            row[3] = itemCodesText
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setRelativeItemsToCategoryFile - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setRelativeItemsToCategoryFile - end."
        }
    }

    def replace = { evt = null ->
        log.info "replace - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                if ( !params.targetFileReplaceColumn ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "翻訳対象列を指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( !params.itemTitleTransFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "商品タイトル翻訳用ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                def ruleFile = new File("${params.itemTitleTransFilePath}")
                if (!( ruleFile.file && ruleFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "商品タイトル翻訳用ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                def rules = []
                ruleFile.withReader(DEFAULT_FILE_ENCODING) {
                    rules.addAll new CSVReader(it, DEFAULT_FILE_TOKEN as char).readAll()
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                // 出力
                def replacedColumn = params.targetFileReplaceColumn.toInteger()

                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())
                        
                        // 商品名置き直し
                        if (row.size() > replacedColumn) {
                            row[replacedColumn - 1] = replaceAllOnRules(row[replacedColumn - 1], rules)
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "replace - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "replace - end."
        }
    }

    String replaceAllOnRules ( sentence, rules ) {
        rules.each {
            sentence = sentence.replaceAll(it[0], it[1])
        }
        sentence
    }

    def showTargetFileSelectedDialog = { evt = null ->
        log.info "showTargetFileSelectedDialog - start."

        try {
            def params = model.properties.clone()

            doOutside {
                def targetFilePath = ""
                def fc = builder.fileChooser(
                    fileSelectionMode:JFileChooser.FILES_ONLY,
                    fileFilter:new FileNameExtensionFilter(
                        "*.csv", "csv", "csv",
                    )
                )

                if(fc.showOpenDialog() == JFileChooser.APPROVE_OPTION) {
                    targetFilePath = fc.selectedFile.absolutePath
                }

                doLater {
                    if (targetFilePath) {
                        model.targetFilePath = targetFilePath
                    }
                }
            }
        } catch ( e ) {
            log.error "showTargetFileSelectedDialog - exception.", e
        } finally {
            log.info "showTargetFileSelectedDialog - end."
        }
    }

    def replaceGeneral = { evt = null ->
        log.info "replaceGeneral - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if ( !params.brandFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "ブランドコード定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def brandFile = new File("${params.brandFilePath}")
                if (!( brandFile.file && brandFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "ブランドコード定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                def brandRows = this.readCsvRows( brandFile ).tail()

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())
                        
                        def replaceColumn = { r, colindex, logic ->
                            if (r.size() > colindex) {
                                r[colindex - 1] = logic(r[colindex - 1], r)
                            }
                        }

                        // 不要な 【,】文字を削除
                        final def columnIndexItemName = 2
                        replaceColumn( row, columnIndexItemName ) { value, r ->
                            value?.replaceAll(/^(.*?)【(.*?)】(.*?)$/, "\$1\$2/\$3")
                        }

                        final def columnIndexHeadline = 9
                        replaceColumn( row, columnIndexHeadline ) { value, r ->
                            return value?.replaceAll(/【|】/, "")
                        }

                        replaceColumn( row, 12 ) { value, r ->
                            return "商品名:${r[1]}"?.toString() ?: ""
                        }

                        final def columnIndexMetaKeyword = 22
                        replaceColumn( row, columnIndexMetaKeyword ) { value, r ->
                            return ( this.pickupCategoryParts(r).join("|") ?: [] )
                        }

                        final def columnIndexDelivery = 35
                        replaceColumn( row, columnIndexDelivery ) { value, r ->
                            def price = r[5]
                            return ( price.isNumber() && price.toInteger() >= 20000 ? "1" : 0 )
                        }

                        final def columnIndexBrandCode = 29
                        replaceColumn( row, columnIndexBrandCode ) { value, r ->
                            def headline = r[columnIndexHeadline - 1]
                            return brandRows.find{ it[1] == headline }?.getAt(0) ?: ""
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "replaceGeneral - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "replaceGeneral - end."
        }
    }

    def setHtmlItems = { evt = null ->
        log.info "setHtmlItems - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if ( !params.htmlFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "HTMLデータ定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def htmlFile = new File("${params.htmlFilePath}")
                if (!( htmlFile.file && htmlFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "HTMLデータ定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                def htmlRows = this.readCsvRows( htmlFile ).tail()

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())
                        
                        def replaceColumn = { r, colindex, logic ->
                            if (r.size() > colindex) {
                                r[colindex - 1] = logic(r[colindex - 1], r)
                            }
                        }

                        final def columnIndexAbstract = 11
                        replaceColumn( row, columnIndexAbstract ) { value, r ->
                            return htmlRows[0][0]
                        }

                        final def columnIndexAdditional1 = 13
                        replaceColumn( row, columnIndexAdditional1 ) { value, r ->
                            return htmlRows[0][1]
                        }

                        final def columnIndexAdditional2 = 14
                        replaceColumn( row, columnIndexAdditional2 ) { value, r ->
                            return htmlRows[0][2]
                        }

                        final def columnIndexAdditional3 = 15
                        replaceColumn( row, columnIndexAdditional3 ) { value, r ->
                            return htmlRows[0][3]
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setHtmlItems - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setHtmlItems - end."
        }
    }

    List<String> pickupCategoryParts( row ) {
        if ( !row[0] )
            return []

        def categoryParts = row[0]?.split("\n")?.collect {
            it?.split(":")
        }?.flatten()?.sort()?.unique()

        return categoryParts ?: []
    }

    def showErrorMessageTargetFilePathIsEmpty() {
        JOptionPane.showOptionDialog(
            app.windowManager.windows[0],
            "商品情報ファイルを指定してください。",
            "エラー",
            JOptionPane.OK_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            [ "閉じる" ] as String[], "閉じる"
        )
    }

    def showErrorMessageTargetFilePathNotExists() {
        JOptionPane.showOptionDialog(
            app.windowManager.windows[0],
            "商品情報ファイルがありません。",
            "エラー",
            JOptionPane.OK_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            [ "閉じる" ] as String[], "閉じる"
        )
    }

    def setShoesSize = { evt = null ->
        log.info "setShoesSize - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if ( !params.shoesSizeFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "靴サイズ定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def shoesSizeFile = new File("${params.shoesSizeFilePath}")
                if (!( shoesSizeFile.file && shoesSizeFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "靴サイズ定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                def old = ""
                def shoesSizeRows = this.readCsvRows( shoesSizeFile ).tail()
                shoesSizeRows = shoesSizeRows.findAll{
                    it[0] || it[1] || it[2] || it[3]
                }.collect{
                    // ブランド名の補完
                    if ( !it[0] ) {
                        it[0] = old
                    } else {
                        old = it[0]
                    }
                    it
                }


                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())

                        def brandName = row[8]
                        def sex = ""
                        if( row[1]?.contains("レディース") ) {
                            sex = "F"
                        } else if( row[1]?.contains("メンズ") ) {
                            sex = "M"
                        }

                        if( sex ) {
                            def sizeMap = [:]
                            shoesSizeRows.eachWithIndex{ r, ridx ->
                                if ( r[0] == brandName && r[2]?.toUpperCase() == sex ) {
                                    ( 3..(r.size()-1) ).each{ cidx ->
                                        sizeMap[r[cidx]] = shoesSizeRows[ridx+1][cidx]
                                    }
                                }
                            }

                            def colorAndSizeInfo = row[7]

                            def sizeInfo = colorAndSizeInfo.replaceAll(/(?ms)(.*?)\n(.*?)\nサイズ (.*)/){ all, l1, l2, l3 ->
                                l1 + "\n" + l2 + "\nサイズ " + l3.split(" ").collect{
                                    sizeMap[it] ? "${it}(${sizeMap[it]}cm)": it
                                }.join(" ")
                            }
                            row[7] = sizeInfo
                        
                            try {
                                def optionInfo = row[3].split("&")*.replaceAll(/#サイズ\:(\d+\.?\d*)=(.+)x\d+\.?\d*x00$/){ all, size, other ->
                                    "#サイズ:" + (sizeMap[size] ? "${size}(${sizeMap[size]}cm)": size) + "=${other}x${(size.toDouble()*10).toInteger().toString().padLeft(3, '0')}x00"
                                }?.join("&") ?: ""
                                row[3] = optionInfo
                            } catch ( NumberFormatException e ) {
                                // 処理を飛ばす
                            }
                        
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setShoesSize - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setShoesSize - end."
        }
    }

    def setProductCode = { evt = null ->
        log.info "setProductCode - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if ( !params.productCodeFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "プロダクトコード定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def productCodeFile = new File("${params.productCodeFilePath}")
                if (!( productCodeFile.file && productCodeFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "プロダクトコード定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                def productCodeRows = this.readCsvRows( productCodeFile ).tail()

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())

                        def itemCode = row[2]

                        def sizeMap = [:]
                        productCodeRows.each{
                            if ( it[0] == itemCode ) {
                                row[30] = it[1]
                            }
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setProductCode - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setProductCode - end."
        }
    }

    def setItemPrice = { evt = null ->
        log.info "setItemPrice - start."

        def progressMonitor = new ProgressMonitor(
            app.windowManager.windows[0], "処理を実行中です。しばらくお待ちください。", "", 0, 100
        )

        try {
            def params = model.properties.clone()

            doOutside {
                if ( !params.targetFilePath ) {
                    showErrorMessageTargetFilePathIsEmpty()
                    return
                }

                def targetFile = new File("${params.targetFilePath}")
                if (!( targetFile.file && targetFile.exists() )) {
                    showErrorMessageTargetFilePathNotExists()
                    return
                }

                if ( !params.itemPriceFilePath ) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "商品価格定義ファイルを指定してください。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                def itemPriceFile = new File("${params.itemPriceFilePath}")
                if (!( itemPriceFile.file && itemPriceFile.exists() )) {
                    JOptionPane.showOptionDialog(
                        app.windowManager.windows[0],
                        "商品価格定義ファイルがありません。",
                        "エラー",
                        JOptionPane.OK_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        [ "閉じる" ] as String[], "閉じる"
                    )
                    return
                }

                if( JOptionPane.showOptionDialog(
                    app.windowManager.windows[0],
                    "実行します。よろしいですか？",
                    "確認",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    [ "はい", "いいえ" ] as String[], "いいえ",
                ) != JOptionPane.YES_OPTION ) {
                    return
                }

                // 入力
                def rows = this.readCsvRows( targetFile )

                def itemPriceRows = this.readCsvRows( itemPriceFile ).tail()

                // バックアップ
                def backupDir = new File("${params.backupDirPath}")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }

                def backupFile = new File("${backupDir.absolutePath}/${targetFile.name}")
                if (backupFile.exists()) { backupFile.delete() }
                targetFile.renameTo(backupFile)
                    
                targetFile.withWriter(DEFAULT_FILE_ENCODING) {
                    def writer = new CSVWriter(it)
                    writer.writeNext(rows.head() as String[])

                    def tails = rows.tail()
                    for( def rowIndex = 0; rowIndex < tails.size(); rowIndex++ ) {
                        log.info "convert ${rowIndex} rows..."

                        def row = tails[rowIndex]

                        if (progressMonitor.isCanceled()) {
                            doLater {
                                JOptionPane.showMessageDialog(
                                    app.windowManager.windows[0],
                                    "処理が中断されました。"
                                )
                            }
                            return
                        }

                        progressMonitor.progress = Math.round(rowIndex * 100 / rows.size())

                        def itemCode = row[2]

                        def sizeMap = [:]
                        itemPriceRows.each{
                            if ( it[0] == itemCode ) {
                                println itemCode
                                row[5] = it[1]
                            }
                        }

                        writer.writeNext(row as String[])
                    }

                }

                doLater {
                    if (!progressMonitor.isCanceled())
                        JOptionPane.showMessageDialog(
                            app.windowManager.windows[0],
                            "処理が完了しました。"
                        )
                }
            }
        } catch ( e ) {
            log.error "setItemPrice - exception.", e
        } finally {
            progressMonitor?.close()
            log.info "setItemPrice - end."
        }
    }

    List readCsvRows( file ) {
        def list = null
        file.withReader(DEFAULT_FILE_ENCODING) {
            list = new CSVReader(it).readAll()
        }
        return list ?: []
    }

}
