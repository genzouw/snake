application {
    title = 'Snake'
    startupGroups = ['snake']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "snake"
    'snake' {
        model      = 'snake.SnakeModel'
        view       = 'snake.SnakeView'
        controller = 'snake.SnakeController'
    }

}
