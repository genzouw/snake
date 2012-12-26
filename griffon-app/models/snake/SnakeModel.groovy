package snake

import groovy.beans.Bindable

class SnakeModel {
    final String appName = "snake"

    @Bindable String targetFilePath = ""

    @Bindable String targetFileReplaceColumn = "2"

    @Bindable String brandFilePath = ""

    @Bindable String htmlFilePath = ""

    @Bindable String itemTitleTransFilePath = ""

    @Bindable String categoryFilePath = ""

    @Bindable String shoesSizeFilePath = ""

    @Bindable String productCodeFilePath = ""

    @Bindable String itemPriceFilePath = ""

    @Bindable String backupDirPath = "./backups"

    @Bindable Boolean editing = true

}
