package snake

application(title: 'snake',
  preferredSize: [550, 850],
  pack: true,
  locationByPlatform:true,
  iconImage: imageIcon('/griffon-icon-48x48.png').image,
  iconImages: [imageIcon('/griffon-icon-48x48.png').image,
               imageIcon('/griffon-icon-32x32.png').image,
               imageIcon('/griffon-icon-16x16.png').image]) {
    // add content here
    borderLayout()
    panel(
        constraints:BorderLayout.CENTER,
    ) {

        migLayout(
            layoutConstraints:"fillx",
            columnConstraints:"[grow][grow]",
        )

        label(
            text:"1.まず、商品情報ファイルを指定してください。（必須）" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
    
        label(
            text:"商品情報ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"targetFilePath",
            text:bind( source:model, sourceProperty:"targetFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:false,
        )
        button(
            text:"ﾌｧｲﾙを開く",
            cssClass:"showDialogButton",
            actionPerformed:controller.showTargetFileSelectedDialog,
            enabled:bind{ model.editing },
            constraints:"wrap",
        )


        label(
            text:"2.翻訳を実施する場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
    
        label(
            text:"翻訳する列" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        label(
            text:"商品情報ファイルの" ,
            constraints:"span 2, split 3",
        )
        textField(
            id:"targetFileReplaceColumn",
            text:bind( source:model, sourceProperty:"targetFileReplaceColumn", mutual:true ),
            columns:1,
            constraints:"",
            dragEnabled:true,
            enabled:bind{ model.editing },
        )
        label(
            text:"列目を翻訳" ,
            constraints:"wrap",
        )
        

        label(
            text:"商品タイトル翻訳用ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"itemTitleTransFilePath",
            text:bind( source:model, sourceProperty:"itemTitleTransFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )

        button(
            text:"実行！",
            actionPerformed:controller.replace,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        


        label(
            text:"3.その他全般の設定を行う場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )


        label(
            text:"ブランドコード定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )
        textField(
            id:"brandFilePath",
            text:bind( source:model, sourceProperty:"brandFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )


        button(
            text:"実行！",
            actionPerformed:controller.replaceGeneral,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        

        label(
            text:"4.関連商品列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        button(
            text:"実行！",
            actionPerformed:controller.setRelativeItems,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        
        label(
            text:"5.HTML列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        label(
            text:"HTMLデータ定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )
        textField(
            id:"htmlFilePath",
            text:bind( source:model, sourceProperty:"htmlFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )
        button(
            text:"実行！",
            actionPerformed:controller.setHtmlItems,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )


        label(
            text:"6.関連商品列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        label(
            text:"カテゴリ関連商品定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"categoryFilePath",
            text:bind( source:model, sourceProperty:"categoryFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )

        button(
            text:"実行！",
            actionPerformed:controller.setRelativeItemsToCategoryFile,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        
    
        label(
            text:"7.靴サイズ情報列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        label(
            text:"靴サイズ定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"shoesSizeFilePath",
            text:bind( source:model, sourceProperty:"shoesSizeFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )

        button(
            text:"実行！",
            actionPerformed:controller.setShoesSize,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        
        label(
            text:"8.プロダクトコード情報列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        label(
            text:"プロダクトコード定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"productCodeFilePath",
            text:bind( source:model, sourceProperty:"productCodeFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )

        button(
            text:"実行！",
            actionPerformed:controller.setProductCode,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        
        
        label(
            text:"9.商品価格情報列をセットする場合はこちら" ,
            cssClass:"subtitle",
            constraints:"grow, span 2, wrap",
        )
        label(
            text:"商品価格定義ファイル" ,
            cssClass:"category",
            constraints:"grow, span 2, wrap",
        )

        textField(
            id:"itemPriceFilePath",
            text:bind( source:model, sourceProperty:"itemPriceFilePath", mutual:true ),
            columns:42,
            constraints:"growx",
            dragEnabled:true,
            enabled:bind( source:model, sourceProperty:"editing" ),
        )

        button(
            text:"実行！",
            actionPerformed:controller.setItemPrice,
            enabled:bind{ model.editing },
            constraints:"span 2, right, wrap",
        )
        
    
    }
}
