log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
        file name:'logfile', file:'snake.log'
    }

    root {
        info 'logfile'
    }

    error  'org.codehaus.griffon'

    info   'griffon.util',
           'griffon.core',
           'griffon.swing',
           'griffon.app'
}


lookandfeel{
    lookAndFeel = 'JTattoo'
    theme = 'Aero'
}

